# FiestaChatArchive

Paper plugin that writes chat and selected command events to a newline-delimited JSON archive. It runs on Paper 1.21 or newer with Java 21. EssentialsX is optional.

It records player chat once, selected player and console chat commands, and server or plugin broadcasts. When EssentialsX is installed, it also records successful private messages, mail, and helpop. Local/global chat events are not written a second time because Paper's chat event already records the message.

Each JSONL record is appended and forced to disk. By default, the archive is `plugins/FiestaChatArchive/chat-events.jsonl`. Set `archive-file` in `config.yml` to an absolute or plugin-relative path. This lets an upgrade continue an existing archive without copying or deleting its history.

The archive can contain private messages, mail, staff messages, and account-related content. Keep it on a private server filesystem and out of public repositories. This repository contains source and tests only; it does not contain live configuration or archive files.

## Build and checks

Set `PLUGIN_API_CLASSPATH` to Paper's API libraries, Gson, and EssentialsX 2.22.0 API classes, then run `./build.sh`. Run `./test.sh` for the command-filter and durable-append checks.

Install the JAR on each Paper server whose chat should be archived. The plugin loads without EssentialsX, which allows it to capture chat on a queue Paper server. Proxy-generated messages are outside Paper's event stream and need an integration in the proxy plugin that sends them.

Fiesta supports offline accounts through a separate authentication setup. This plugin archives messages; it does not authenticate accounts.
