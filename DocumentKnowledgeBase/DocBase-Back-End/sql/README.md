# SQL Usage

This directory is now prepared for public repository use.

## Public entrypoint

For a fresh local database, use:

- `docbase_knowledge_public_bootstrap.sql`

This file contains:

- all required tables for the current Java project
- the key fields required by AI chat and ingest task features
- minimal demo data only
- no private chat history, private knowledge files, or local business data

## Recommended usage

### 1. Initialize a brand-new local database

```bash
cd D:\ResumeProjects\DocumentKnowledgeBase\DocBase-Back-End\sql
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS docbase_knowledge DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;"
mysql -u root -p docbase_knowledge < docbase_knowledge_public_bootstrap.sql
```

Or:

```bash
cd D:\ResumeProjects\DocumentKnowledgeBase\DocBase-Back-End\sql
mysql -u root -p < docbase_knowledge_public_bootstrap.sql
```

### 2. Existing private/local migration scripts

This public repository no longer treats the historical SQL scripts as the default startup path.

If you still keep older private/local migration scripts on your own machine, they are for:

- local historical environment repair
- internal migration replay
- private data recovery/testing

They should not be the default path for people cloning the public repository.

## What is intentionally not included in the public SQL

The public bootstrap SQL does not contain:

- real users
- private email / phone / avatar paths
- login logs
- operation logs
- AI chat history
- AI audit history
- private knowledge documents
- private upload paths
- private ingest task history
- private vectorization data

## Public repository rule

For the public repo, the `sql` directory should mainly expose:

- `.gitignore`
- `README.md`
- `docbase_knowledge_public_bootstrap.sql`

Everything else should stay local only if it contains private or historical data.
