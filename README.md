# Mac Game Compatibility Database

*(A better name may be pending)*

Capstone project for a BA, Software Development for [Garrett Albright](https://albright.pro)

A database of games on the Steam storefront which can be played on macOS computers, with a specific emphasis on whether the game is playable on newer operating systems which only support 64-bit applications, as this information on Steam’s storefront is often incorrect.

Web application built with Java 11 and Maven. SQLite 3 is used for the database.

## Concepts

The same project will run the web application as a persistent daemon, as well as run "tasks" and quit. Tasks are things like fetching new games, updating existing games, etc, and intended to be run as cron jobs.

To run the web daemon, simply invoke MGCDB without any parameters. Adding a parameter will attempt to run the task indicated by that parameter, and additional parameters may affect the behavior of the task. Tasks and their parameters are described below.

## Configuration

Configuration is managed via a simple [TOML](https://github.com/toml-lang/toml) file which is checked for in the following locations, in order:

* `/etc/mgcdb/conf.toml`
* `~/.config/mgcdb_conf.toml`
* The sample `conf.toml` at the root level of this repo.

To configure MGCDB, copy the sample `conf.toml` to the desired spot and edit it. All possible configuration directives are listed in the file and documented with comments.

TODO: What about Windows users?

TODO: Specify configuration file location via CLI?

## Web daemon

The web daemon will run on port 4567 by default (this can be changed in the config file (TODO: actually implement that)). It's intended that another web daemon reverse proxy this and also serve static files from the `static` directory because the web daemon will not attempt to serve those. Example Nginx configuration is shown below (possibly desirable directives such as `error_log` omitted for brevity):

```nginx
server {
  listen 80;
  server_name mgcdb.test;

  location /static/ {
    alias /path/to/mgcdb/static/;
  }

  location / {
    proxy_pass http://localhost:4567;
  }
}
```

For other web daemons, please refer to its documentation to see how to configure it similarly.

## Tasks

### `initdb`

Initializes the database by creating the required tables. The location for the database file can be specified in the configuration file. Remember the file and its enclosing directory should be writable by whatever user you're running tasks (and the web daemon itself) as.

The parameter `delete` may be passed, in which case *any existing database file will be deleted* and recreated from scratch (*all* data will be lost). Otherwise, if a database file already exists, this task will attempt to create any tables which do not already exist in it.

### `newgames`

Fetches information for games which are not currently in the database from Steam. Optionally a second parameter with the number of new games to fetch, up to 50,000; currently fetches 200 by default (note it will fetch fewer if there aren't that many new games to fetch).

### `updategames`

Updates games with data from the Steam database. Note that the `newgames` task only fetches some initial data which isn't very useful, so `updategames` should usually be invoked right after `newgames`. Pass an optional second parameter with the number of games to update, up to 200; it will update 100 by default. Note that the Steam API will only permit you to get information on 200 games every 5 minutes; if you see the error "Unexpected status code 429 while making Steam request" while running this task, you need to slow down, bucko.

MGCDB will automatically select which games to find updates for based on which games are new and haven't been updated yet (eg, they were just inserted with the `newgames` task), followed by those that have gone the longest since their last update. Games which have been updated in the last 24 hours will never be included.

## Step-by-step installation & operation

1. Ensure you have Java 11 or newer and Maven installed on your system via your package/port manager of choice.
2. Clone this Git repository and `cd` into the directory.
4. Copy and edit the configuration file (see "Configuration" above).
6. Build a JAR: `mvn clean package assembly:single`. The JAR will be in the `target` directory. Optionally move it to a more convenient spot.
7. Initialize the database: `java -jar path/to/mgcdb.jar initdb`
    1. If you are also using OpenBSD and this fails with a dumped error trace that includes "Caused by: java.lang.Exception: No native library found for os.name=OpenBSD, os.arch=x86_64, paths=[/usr/lib:/usr/local/lib]", do the following (this procedure may work with other OSes as well; YMMV):
    1. Ensure you've installed the `comp` set during installation. If you can't recall if you did or not, then you probably did. If you know that you didn't, or you get some messages later about tools like `gcc` being missing, you can install sets post-installation using [this procedure](https://www.cyberciti.biz/faq/openbsd-install-sets-after-install/).
    2. Install the `gmake`, `bash`, and `unzip` packages.
    3. In `/tmp` or some other convenient place, clone the [SQLite JDBC driver](https://github.com/xerial/sqlite-jdbc) repository (`git@github.com:xerial/sqlite-jdbc.git`) and `cd` into it.
    4. Run `gmake` (note: not `make` as the makefile is not compatible with OpenBSD's standard `make`). The build process will eventually error out with some nonsense having to do with Docker, but that's fine - it got far enough.
    5. Copy the file at `target/sqlite-[some version number]-OpenBSD-x86_64/libsqlitejdbc.so` into `/usr/local/lib`.
    6. Optionally uninstall the packages and repo installed/cloned earlier - neither are needed further.
    7. Try the database initialization command again.
8. Fetch some games: `java -jar path/to/mgcdb.jar newgames`. Note that this will not fetch every game currently on Steam, but will give you a few to experiment with.
9. Fetch details for those games: `java -jar path/to/mgcdb.jar updategames`
10. Start the web daemon: `java -jar path/to/mgcdb.jar` (no further parameters). Test by accessing [http://localhost:4567](http://localhost:4567) in your browser of choice (adjust the port number if you set a custom port number in your configuration file).
11. Set up your web server to reverse proxy the web daemon and serve MGCDB's static files (see "Web daemon" above).
12. Configure a cron job task to run the `newgames` and `updategames` tasks periodically (preferably in that order). See the "Tasks" section above for more information.


## Legal nonsense

This project is not created or authorized by Valve Corporation, the operators of Steam. Please do not contact them with questions related to this project. Create a GitHub issue for this project instead, or contact me via email at contact@albright.pro .

All game information, including titles, descriptions, and images, should be considered the property of the respective games' publishers and reused by MGCDB for informational purposes only (and to help increase their sales and reduce their refunds!). No claim as to the ownership of this information is to be inferred.

Special thanks to the members of the ##java and #sqlite channels on Freenode for help throughout this learning experience.

Copyright 2020 Garrett Albright

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
