# Hydra Notifications

A generic HTTP container that sends events to underlying services.

# Supported Services

Currently, two services are supported: Slack and OpsGenie.

Others forthcoming.

## Adding a new service

#### Create the service actor

Look at any of the service classes under services/hydra/notifications/services.

For instance:

`hydra.notifications.services.Slack`

Services are Akka actors tagged with the `HydraNotificationService` trait, which allows them to be loaded dynamically at 
start up time.

> The lower case short name of the service class becomes the service name. For instance, `hydra.notifications.services.Slack` will be available as "slack"


#### Create the service notification case class 

Make sure it extends from `http.hydra.notifications.HydraNotification`

Examples: SlackNotification and OpsGenieNotification


#### Add the case class to the SprayJsonFormat 

Modify `http.hydra.notifications.http.NotificationsFormat.scala` and add your service name to the pattern match statements

# Example calls

## Slack

```bash
 curl -X POST localhost:8080 -d '{"channel":"hydra-ops","message":"an error occured","service":"slack"}'  -H "Content-Type:application/json"
```
 
## Ops Genie
 
```bash
  curl -X POST localhost:8080 -d '{"source":"source","description":"description","tags":["tag1","tag2"],"service":"opsgenie","alias":"alias","note":"note","team":"team","entity":"entity","message":"message","user":"user"}' -H "Content-Type:application/json"
``` 

# Building
 `sbt universal:packageBin`
 
 The generated zip file will contain both the assembled jar and the run script.
 
# Running the App

Supply authentication tokens for Slack and OpsGenie by using one of these methods:
- Modify `reference.conf` to add authentication tokens for Slack and OpsGenie.
- Supply environment variables `OPSGENIE_TOKEN` and `SLACK_TOKEN`
- Add the tokens to `/etc/hydra/hydra-notifications.conf`

Unzip the executable generated by sbt and run ./bin/hydra-notifications.

By default, the container binds to port 8080.  To change that, supply this configuration property:

`container.http.port=$PORT_NUMBER`
