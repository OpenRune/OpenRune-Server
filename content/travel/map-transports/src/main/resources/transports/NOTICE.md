# Transport data

These tables come from the [Shortest Path RuneLite plugin](https://github.com/Skretzo/shortest-path)
and are used under the BSD 2-Clause licence in `LICENSE-shortest-path`. `COMMIT` records the
upstream commit. The import resolves references using the current OSRS revision's gamevals.

Run `./gradlew :or-cache:downloadTransports` to refresh the tables manually. Revision sync runs
the same task and commits the results. The server and PR gameval checks use the committed tables.
