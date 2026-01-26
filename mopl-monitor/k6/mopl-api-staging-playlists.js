import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://mopl-alb-1366626516.ap-northeast-2.elb.amazonaws.com';
const DEFAULT_ACCOUNTS = [
  { email: 'admin@mopl.io', password: '1234' },
  { email: 'woody@mopl.io', password: '1234' },
  { email: 'buzz@mopl.io', password: '1234' },
  { email: 'jessie@mopl.io', password: '1234' },
  { email: 'rex@mopl.io', password: '1234' },
  { email: 'slinky@mopl.io', password: '1234' },
];

function loadAccounts() {
  if (__ENV.K6_ACCOUNTS_JSON) {
    try {
      const parsed = JSON.parse(__ENV.K6_ACCOUNTS_JSON);
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed;
      }
    } catch (err) {
      console.warn('K6_ACCOUNTS_JSON parse failed, using defaults.', err);
    }
  }

  const count = Number(__ENV.K6_TEST_USER_COUNT || 0);
  if (count > 0) {
    const prefix = __ENV.K6_TEST_USER_PREFIX || 'loadtest-';
    const domain = __ENV.K6_TEST_USER_DOMAIN || 'mopl.io';
    const password = __ENV.K6_TEST_USER_PASSWORD || '1234';
    const accounts = [];
    for (let i = 0; i < count; i += 1) {
      const email = `${prefix}${String(i).padStart(5, '0')}@${domain}`;
      accounts.push({ email, password });
    }
    return accounts;
  }

  return DEFAULT_ACCOUNTS;
}

const ACCOUNTS = loadAccounts();
const MODE = __ENV.K6_MODE || 'vus';
const RPS_START_RATE = Number(__ENV.K6_RPS_START_RATE || 50);
const RPS_TIME_UNIT = __ENV.K6_RPS_TIME_UNIT || '1s';
const RPS_PREALLOCATED_VUS = Number(__ENV.K6_RPS_PREALLOCATED_VUS || 120);
const RPS_MAX_VUS = Number(__ENV.K6_RPS_MAX_VUS || 240);

function parseRpsStages(value, fallbackStages) {
  if (!value) {
    return fallbackStages;
  }
  const stages = value
    .split(',')
    .map((entry) => entry.trim())
    .filter(Boolean)
    .map((entry) => {
      const [targetRaw, durationRaw] = entry.split(':').map((part) => part.trim());
      const target = Number(targetRaw);
      if (!Number.isFinite(target) || !durationRaw) {
        console.warn(`Invalid K6_RPS_STAGES entry: "${entry}"`);
        return null;
      }
      return { target, duration: durationRaw };
    })
    .filter(Boolean);

  return stages.length > 0 ? stages : fallbackStages;
}

const VUS_SCENARIOS = {
  baseline: {
    executor: 'constant-vus',
    vus: 30,
    duration: '10m',
  },
  sustained: {
    executor: 'constant-vus',
    vus: 60,
    duration: '25m',
    startTime: '10m',
  },
  stress: {
    executor: 'constant-vus',
    vus: 100,
    duration: '10m',
    startTime: '35m',
  },
};

const RPS_SCENARIOS = {
  rps_probe: {
    executor: 'ramping-arrival-rate',
    startRate: RPS_START_RATE,
    timeUnit: RPS_TIME_UNIT,
    preAllocatedVUs: RPS_PREALLOCATED_VUS,
    maxVUs: RPS_MAX_VUS,
    stages: parseRpsStages(__ENV.K6_RPS_STAGES, [
      { target: 150, duration: '5m' },
      { target: 200, duration: '5m' },
      { target: 250, duration: '5m' },
      { target: 300, duration: '5m' },
    ]),
  },
};

export const options = {
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1200'],
  },
  scenarios: MODE === 'rps' ? RPS_SCENARIOS : VUS_SCENARIOS,
};

function getCsrfToken() {
  const res = http.get(`${BASE_URL}/api/auth/csrf-token`, { tags: { name: 'csrf_token' } });
  check(res, { 'csrf status ok': (r) => r.status === 200 || r.status === 204 });
  const cookie = res.cookies && res.cookies['XSRF-TOKEN'] && res.cookies['XSRF-TOKEN'][0];
  return cookie ? cookie.value : '';
}

function signIn(account) {
  const csrfToken = getCsrfToken();
  const body = `username=${encodeURIComponent(account.email)}&password=${encodeURIComponent(account.password)}`;
  const res = http.post(`${BASE_URL}/api/auth/sign-in`, body, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'X-XSRF-TOKEN': csrfToken,
    },
    tags: { name: 'sign_in' },
  });
  check(res, { 'sign-in ok': (r) => r.status === 200 });
  const json = res.json() || {};
  return {
    token: json.accessToken || '',
    userId: json.userDto && json.userDto.id ? json.userDto.id : '',
  };
}

export function setup() {
  if (!ACCOUNTS.length) {
    console.warn('No accounts configured for k6.');
    return [];
  }
  return ACCOUNTS.map((account) => signIn(account));
}

function authHeaders(token) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  };
}

function listPlaylists(token) {
  const params = 'limit=20&sortDirection=DESCENDING&sortBy=updatedAt';
  const res = http.get(`${BASE_URL}/api/playlists?${params}`, {
    ...authHeaders(token),
    tags: { name: 'playlists_list' },
  });
  check(res, { 'playlists list ok': (r) => r.status === 200 });
  return res;
}

export default function (data) {
  if (!data || !data.length) {
    sleep(1);
    return;
  }
  const auth = data[(__VU - 1) % data.length] || {};
  const token = auth.token;

  if (!token) {
    return;
  }

  listPlaylists(token);

  sleep(0.3 + Math.random() * 0.4);
}
