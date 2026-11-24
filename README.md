# FiestaChatArchive

Paper plugin source for archiving chat events. Version in plugin metadata: 1.1.0. Requires Paper 1.21 or newer, Java 21, and EssentialsX.

It records player chat, Essentials local and global chat, private messages, mail, help requests, broadcasts, and selected chat commands. Each event is appended as JSONL and forced to disk. The archive is written under the plugin's own data folder.

The archive can contain private messages, email, staff chat, and account-related content. Treat its data folder as private server data. This repository contains source and tests only; do not commit live configuration or archive files.

## Build and checks

Set PLUGIN_API_CLASSPATH to Paper's API libraries, EssentialsX 2.22.0 API classes, and Gson. Run ./build.sh.

Run ./test.sh to compile and run the chat-command and durable-append checks.

## Account wording

Fiesta supports offline accounts through a separate authentication setup. This plugin archives messages; it does not authenticate accounts.
