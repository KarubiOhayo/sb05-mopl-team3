import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://mopl-alb-1366626516.ap-northeast-2.elb.amazonaws.com';
const WS_URL = __ENV.WS_URL || toWsUrl(BASE_URL, '/ws/websocket');
const DEFAULT_ACCOUNTS = [
  { email: 'admin@mopl.io', password: '1234' },
  { email: 'woody@mopl.io', password: '1234' },
  { email: 'buzz@mopl.io', password: '1234' },
  { email: 'jessie@mopl.io', password: '1234' },
  { email: 'rex@mopl.io', password: '1234' },
  { email: 'slinky@mopl.io', password: '1234' },
];

const WS_CONNECTS = new Counter('k6_ws_connects');
const WS_CONNECT_ERRORS = new Counter('k6_ws_connect_errors');
const WS_MESSAGES_SENT = new Counter('k6_ws_msgs_sent');
const WS_MESSAGES_RECEIVED = new Counter('k6_ws_msgs_received');
const WS_STOMP_ERRORS = new Counter('k6_ws_stomp_errors');
const SSE_CONNECTS = new Counter('k6_sse_connects');
const SSE_ERRORS = new Counter('k6_sse_errors');

const MODE = __ENV.K6_SOCKET_MODE || 'ws';
const TOPIC_MODE = __ENV.K6_SOCKET_TOPIC_MODE || 'mix';
const SSE_URL = __ENV.SSE_URL || `${BASE_URL}/api/sse`;
const WS_MESSAGE_INTERVAL_MS = Number(__ENV.K6_WS_MSG_INTERVAL_MS || 3000);
const WS_HOLD_VUS = Number(__ENV.K6_SOCKET_WS_HOLD_VUS || 0);
const WS_HOLD_DURATION = __ENV.K6_SOCKET_WS_HOLD_DURATION || '10m';
const WS_HOLD_RAMP = __ENV.K6_SOCKET_WS_HOLD_RAMP || '5m';

const ENABLE_WS = MODE === 'ws' || MODE === 'both' || MODE === 'mix';
const ENABLE_REST = MODE === 'rest' || MODE === 'both' || MODE === 'mix';
const ENABLE_SSE = MODE === 'sse' || MODE === 'both' || MODE === 'mix';

const scenarios = {};
if (ENABLE_WS) {
  const holdStages =
    WS_HOLD_VUS > 0
      ? [
          { duration: WS_HOLD_RAMP, target: WS_HOLD_VUS },
          { duration: WS_HOLD_DURATION, target: WS_HOLD_VUS },
        ]
      : null;
  const defaultRampStages =
    __ENV.K6_SOCKET_WS_RAMP === 'true' || __ENV.K6_SOCKET_WS_RAMP === '1'
      ? [
          { duration: '5m', target: 30 },
          { duration: '5m', target: 60 },
          { duration: '5m', target: 100 },
        ]
      : null;
  const wsStages = parseStages(__ENV.K6_SOCKET_WS_STAGES, holdStages || defaultRampStages);
  if (wsStages) {
    scenarios.socket_ws = {
      executor: 'ramping-vus',
      stages: wsStages,
      gracefulStop: __ENV.K6_SOCKET_WS_GRACEFUL_STOP || '30s',
      exec: 'socketScenario',
    };
  } else {
    scenarios.socket_ws = {
      executor: 'constant-vus',
      vus: Number(__ENV.K6_SOCKET_VUS || 30),
      duration: __ENV.K6_SOCKET_DURATION || '15m',
      exec: 'socketScenario',
    };
  }
}
if (ENABLE_REST) {
  scenarios.socket_rest = {
    executor: 'constant-vus',
    vus: Number(__ENV.K6_SOCKET_REST_VUS || 20),
    duration: __ENV.K6_SOCKET_REST_DURATION || '15m',
    exec: 'restScenario',
  };
}
if (ENABLE_SSE) {
  scenarios.socket_sse = {
    executor: 'constant-vus',
    vus: Number(__ENV.K6_SOCKET_SSE_VUS || 20),
    duration: __ENV.K6_SOCKET_SSE_DURATION || '10m',
    exec: 'sseScenario',
  };
}
if (Object.keys(scenarios).length === 0) {
  scenarios.socket_ws = {
    executor: 'constant-vus',
    vus: 10,
    duration: '5m',
    exec: 'socketScenario',
  };
}

export const options = {
  thresholds: {
    'http_req_failed{scenario:socket_ws}': ['rate<0.02'],
    'http_req_failed{scenario:socket_rest}': ['rate<0.02'],
    'http_req_failed{scenario:socket_sse}': ['rate<0.5'],
  },
  scenarios,
};

function toWsUrl(baseUrl, path) {
  const normalized = baseUrl.replace(/\/+$/, '');
  if (normalized.startsWith('https://')) {
    return `wss://${normalized.substring('https://'.length)}${path}`;
  }
  if (normalized.startsWith('http://')) {
    return `ws://${normalized.substring('http://'.length)}${path}`;
  }
  return `${normalized}${path}`;
}

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

function getCsrfToken() {
  const res = http.get(`${BASE_URL}/api/auth/csrf-token`);
  const cookie = res.cookies && res.cookies['XSRF-TOKEN'] && res.cookies['XSRF-TOKEN'][0];
  return cookie ? cookie.value : '';
}

function signIn(account) {
  const csrfToken = getCsrfToken();
  const body = `username=${encodeURIComponent(account.email)}&password=${encodeURIComponent(
    account.password
  )}`;
  const res = http.post(`${BASE_URL}/api/auth/sign-in`, body, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'X-XSRF-TOKEN': csrfToken,
    },
  });
  check(res, { 'sign-in ok': (r) => r.status === 200 });
  const json = res.json() || {};
  return {
    token: json.accessToken || '',
    userId: json.userDto && json.userDto.id ? json.userDto.id : '',
  };
}

function fetchContentIds(token) {
  if (__ENV.K6_CONTENT_IDS) {
    return __ENV.K6_CONTENT_IDS.split(',').map((id) => id.trim()).filter(Boolean);
  }
  const params = 'limit=20&sortDirection=DESCENDING&sortBy=createdAt';
  const res = http.get(`${BASE_URL}/api/contents?${params}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (res.status !== 200) {
    return [];
  }
  const json = res.json() || {};
  const data = Array.isArray(json.data) ? json.data : [];
  return data.map((item) => item.id).filter(Boolean);
}

function fetchConversationIds(token) {
  if (__ENV.K6_CONVERSATION_IDS) {
    return __ENV.K6_CONVERSATION_IDS.split(',').map((id) => id.trim()).filter(Boolean);
  }
  const params = 'limit=20&sortDirection=DESCENDING&sortBy=createdAt';
  const res = http.get(`${BASE_URL}/api/conversations?${params}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (res.status !== 200) {
    return [];
  }
  const json = res.json() || {};
  const data = Array.isArray(json.data) ? json.data : [];
  return data.map((item) => item.id).filter(Boolean);
}

export function setup() {
  const accounts = loadAccounts();
  if (!accounts.length) {
    console.warn('No accounts configured for socket test.');
    return { auths: [], contentIds: [], conversationIds: [] };
  }

  const auths = accounts.map((account) => signIn(account)).filter((auth) => auth.token);
  const seedToken = auths[0] ? auths[0].token : '';
  const contentIds = seedToken ? fetchContentIds(seedToken) : [];
  const conversationIds = seedToken ? fetchConversationIds(seedToken) : [];

  return { auths, contentIds, conversationIds };
}

function pickId(list) {
  if (!list || list.length === 0) return '';
  return list[Math.floor(Math.random() * list.length)];
}

function stompFrame(command, headers, body) {
  const lines = [command];
  Object.keys(headers || {}).forEach((key) => {
    lines.push(`${key}:${headers[key]}`);
  });
  lines.push('');
  lines.push(body || '');
  return `${lines.join('\n')}\u0000`;
}

function buildSubscriptions(topicMode, contentId, conversationId) {
  if (topicMode === 'watch') {
    return [{ id: 'sub-watch', dest: `/sub/contents/${contentId}/watch` }];
  }
  if (topicMode === 'chat') {
    return [{ id: 'sub-chat', dest: `/sub/contents/${contentId}/chat` }];
  }
  if (topicMode === 'dm') {
    return [{ id: 'sub-dm', dest: `/sub/conversations/${conversationId}/direct-messages` }];
  }
  return [
    { id: 'sub-watch', dest: `/sub/contents/${contentId}/watch` },
    { id: 'sub-chat', dest: `/sub/contents/${contentId}/chat` },
    { id: 'sub-dm', dest: `/sub/conversations/${conversationId}/direct-messages` },
  ];
}

export function socketScenario(data) {
  const auths = data.auths || [];
  const contentIds = data.contentIds || [];
  const conversationIds = data.conversationIds || [];
  if (!auths.length || !contentIds.length) {
    sleep(1);
    return;
  }

  const auth = auths[(__VU - 1) % auths.length];
  const contentId = pickId(contentIds);
  const conversationId = pickId(conversationIds);

  const params = {
    tags: { name: 'socket_ws' },
  };

  const res = ws.connect(WS_URL, params, (socket) => {
    let connected = false;
    let subscriptionSent = false;

    socket.on('open', () => {
      WS_CONNECTS.add(1);
      socket.send(
        stompFrame(
          'CONNECT',
          {
            'accept-version': '1.2',
            'heart-beat': '10000,10000',
            Authorization: `Bearer ${auth.token}`,
          },
          ''
        )
      );
    });

    socket.on('message', (data) => {
      const text = String(data || '');
      if (text.includes('CONNECTED')) {
        connected = true;
      }

      if (connected && !subscriptionSent) {
        const subs = buildSubscriptions(TOPIC_MODE, contentId, conversationId);
        subs.forEach((sub) => {
          socket.send(
            stompFrame('SUBSCRIBE', { id: sub.id, destination: sub.dest }, '')
          );
        });
        subscriptionSent = true;
      }

      if (text.includes('MESSAGE')) {
        WS_MESSAGES_RECEIVED.add(1);
      }
    });

    socket.on('error', () => {
      WS_CONNECT_ERRORS.add(1);
    });

    socket.setInterval(() => {
      if (!connected) {
        return;
      }

      if (TOPIC_MODE === 'chat' || TOPIC_MODE === 'mix') {
        const destination = `/pub/contents/${contentId}/chat`;
        socket.send(
          stompFrame('SEND', { destination }, JSON.stringify({ content: 'hello' }))
        );
        WS_MESSAGES_SENT.add(1);
      }

      if (TOPIC_MODE === 'dm' || TOPIC_MODE === 'mix') {
        if (conversationId) {
          const destination = `/pub/conversations/${conversationId}/direct-messages`;
          socket.send(
            stompFrame('SEND', { destination }, JSON.stringify({ content: 'hello' }))
          );
          WS_MESSAGES_SENT.add(1);
        } else {
          WS_STOMP_ERRORS.add(1);
        }
      }
    }, WS_MESSAGE_INTERVAL_MS);

    socket.setTimeout(() => {
      socket.close();
    }, 15000);
  });

  check(res, { 'ws connected': (r) => r && r.status === 101 });
  sleep(1);
}

export function restScenario(data) {
  const auths = data.auths || [];
  const contentIds = data.contentIds || [];
  if (!auths.length || !contentIds.length) {
    sleep(1);
    return;
  }

  const auth = auths[(__VU - 1) % auths.length];
  const watcherId = auth.userId;
  const contentId = pickId(contentIds);
  if (!watcherId || !contentId) {
    sleep(1);
    return;
  }

  const limit = Number(__ENV.K6_WATCHING_SESSIONS_LIMIT || 20);
  const cursor = __ENV.K6_WATCHING_SESSIONS_CURSOR || '';
  const cursorParam = cursor ? `&cursor=${encodeURIComponent(cursor)}` : '';

  const resUser = http.get(
    `${BASE_URL}/api/users/${watcherId}/watching-sessions?limit=${limit}${cursorParam}`,
    {
      headers: { Authorization: `Bearer ${auth.token}` },
      tags: { name: 'watching_sessions_by_user' },
    }
  );
  check(resUser, { 'watching sessions by user ok': (r) => r.status === 200 });

  const resContent = http.get(
    `${BASE_URL}/api/contents/${contentId}/watching-sessions?limit=${limit}${cursorParam}`,
    {
      headers: { Authorization: `Bearer ${auth.token}` },
      tags: { name: 'watching_sessions_by_content' },
    }
  );
  check(resContent, { 'watching sessions by content ok': (r) => r.status === 200 });

  sleep(1);
}

export function sseScenario(data) {
  const auths = data.auths || [];
  if (!auths.length) {
    sleep(1);
    return;
  }

  const auth = auths[(__VU - 1) % auths.length];
  const timeout = __ENV.K6_SSE_TIMEOUT || '60s';
  SSE_CONNECTS.add(1);
  const res = http.get(SSE_URL, {
    headers: {
      Authorization: `Bearer ${auth.token}`,
      Accept: 'text/event-stream',
    },
    tags: { name: 'sse_subscribe' },
    timeout,
  });

  if (!res || res.status !== 200) {
    SSE_ERRORS.add(1);
  }
  sleep(1);
}

export default function (data) {
  if (ENABLE_WS) {
    socketScenario(data);
  } else {
    sleep(1);
  }
}

function parseStages(raw, fallback) {
  if (raw && raw.trim()) {
    const stages = raw
      .split(',')
      .map((part) => part.trim())
      .filter(Boolean)
      .map((part) => {
        const [duration, target] = part.split(':');
        return { duration: duration.trim(), target: Number(target) };
      })
      .filter((stage) => stage.duration && Number.isFinite(stage.target));
    return stages.length ? stages : fallback;
  }
  return fallback;
}
