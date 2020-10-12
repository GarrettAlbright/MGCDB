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

Warning: If you have a currently-existing database, this command will blow it away and recreate it fresh with zero warning!

### `newgames`

Fetches information for games which are not currently in the database from Steam. Optionally a second parameter with the number of new games to fetch, up to 50,000; currently fetches 100 (TODO: Update) by default (note it will fetch fewer if there aren't that many new games to fetch).

### `updategames`

Updates games with data from the Steam database. Note that the `newgames` task only fetches some initial data which isn't very useful, so `updategames` should usually be invoked right after `newgames`. Pass an optional second parameter with the number of games to update, up to 200; it will update 100 by default. Note that the Steam API will only permit you to get information on 200 games every 5 minutes; if you see the error "Unexpected status code 429 while making Steam request" while running this task, you need to slow down, bucko.

MGCDB will automatically select which games to find updates for based on which games are new and haven't been updated yet (eg, they were just inserted with the `newgames` task), followed by those that have gone the longest since their last update. Games which have been updated in the last 24 hours will never be included.

## Step-by-step operation

1. Ensure you have Java 11 or newer and Maven installed on your system via your package/port manager of choice.
2. Clone the project.
3. `cd` into project directory.
4. Copy and edit the configuration file (see "Configuration" above).
6. Build a JAR: `mmvn clean package assembly:single`. The JAR will be in the `target` directory. Optionally move it to a more convenient spot.
7. Initialize the database: `java -jar path/to/mgcdb.jar initdb`
8. Fetch some games: `java -jar path/to/mgcdb.jar newgames`. Note that this will not fetch every game currently on Steam, but will give you a few to experiment with.
9. Fetch details for those games: `java -jar path/to/mgcdb.jar updategames`
10. Start the web daemon: `java -jar path/to/mgcdb.jar` (no further parameters). Test by accessing [http://localhost:4567](http://localhost:4567) in your browser of choice (adjust the port number if you set a custom port number in your configuration file).
11. Set up your web server to reverse proxy the web daemon and serve MGCDB's static files (see "Web daemon" above).
12. Configure a cron job task to run the `newgames` and `updategames` tasks periodically (preferably in that order).


## Legal nonsense

This project is not created or authorized by Valve Corporation, the operators of Steam. Please do not contact them with questions related to this project. Create a GitHub issue for this project instead, or contact me via email at contact@albright.pro .

All game information, including titles, descriptions, and images, should be considered the property of the respective games' publishers and reused by MGCDB for informational purposes only (and to help increase their sales and reduce their refunds!). No claim as to the ownership of this information is to be inferred.

MGCDB is licensed under the BSD 2-clause license. Various libraries used by MGCDB (see 
pom.xml) will have their own license terms but all are FOSS-licensed.
