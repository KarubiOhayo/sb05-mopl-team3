# mopl-seeder

Local-only seeding tool for staging data. This module is not intended for deployment.

## Run

```bash
./gradlew :mopl-seeder:seedStaging
```

## Notes

- Uses environment variables for DB connection and seeding options.
- Safe for local use only; do not include this module in deployable images.
