/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.cataloguefrontend

import java.time.LocalDateTime

import uk.gov.hmrc.cataloguefrontend.model.Environment
import uk.gov.hmrc.cataloguefrontend.shuttering.{ShutterStatusValue, ShutterType}

/**
  * Created by armin.
  */
object JsonData {
  val digitalServiceNamesData =
    """
      |[
      |   "digital-service-1",
      |   "digital-service-2",
      |   "digital-service-3"
      |]
    """.stripMargin

  val teamsWithRepos =
    """
      |[
      |  {
      |    "name": "team1",
      |    "repos": {
      |      "Service": [
      |        "service1",
      |        "service2"
      |      ],
      |      "Library": ["lib1", "lib2"],
      |      "Prototype": [],
      |      "Other": [
      |        "other1",
      |        "other2"
      |      ]
      |    },
      |    "ownedRepos" : []
      |  },
      |  {
      |    "name": "team2",
      |    "repos": {
      |      "Service": [
      |        "service3",
      |        "service4"
      |      ],
      |      "Library": ["lib3", "lib4"],
      |      "Prototype": ["prototype1"],
      |      "Other": [
      |        "other3",
      |        "other4"
      |      ]
      |    },
      |    "ownedRepos" : []
      |  }
      |]
    """.stripMargin

  import DateHelper._

  val createdAt    = LocalDateTime.of(2016, 5, 23, 16, 45, 30)
  val lastActiveAt = LocalDateTime.of(2016, 10, 12, 10, 30, 12)

  val repositoriesData =
    s"""|[
        |{"name":"teamA-serv", "createdAt": ${createdAt.epochMillis}, "lastUpdatedAt": ${lastActiveAt.epochMillis}, "repoType":"Service"},
        |{"name":"teamB-library", "createdAt": ${createdAt.epochMillis}, "lastUpdatedAt": ${lastActiveAt.epochMillis}, "repoType":"Library"},
        |{"name":"teamB-other", "createdAt": ${createdAt.epochMillis}, "lastUpdatedAt": ${lastActiveAt.epochMillis}, "repoType":"Other"}
        |]""".stripMargin

  val serviceDetailsData =
    s"""
       |{
       |  "name": "service-1",
       |  "isPrivate": false,
       |  "isArchived": false,
       |  "description": "some description",
       |  "createdAt": ${createdAt.epochMillis},
       |  "lastActive": ${lastActiveAt.epochMillis},
       |  "repoType": "Service",
       |  "owningTeams": [
       |    "The True Owners"
       |  ],
       |  "teamNames": ["teamA", "teamB"],
       |  "githubUrl": {
       |    "name": "github",
       |    "displayName": "github.com",
       |    "url": "https://github.com/hmrc/service-1"
       |  },
       |  "environments" : [
       |    {
       |      "name" : "Development",
       |      "services" : [
       |        {
       |          "name": "jenkins",
       |          "displayName": "Jenkins",
       |          "url": "https://deploy-dev.co.uk/job/deploy-microservice"
       |        }, {
       |          "name": "grafana",
       |          "displayName": "Grafana",
       |          "url": "https://grafana-dev.co.uk/#/dashboard"
       |        }
       |      ]
       |    }, {
       |      "name" : "QA",
       |      "services" : [
       |        {
       |          "name": "jenkins",
       |          "displayName": "Jenkins",
       |          "url": "https://deploy-qa.co.uk/job/deploy-microservice"
       |        }, {
       |          "name": "grafana",
       |          "displayName": "Grafana",
       |          "url": "https://grafana-datacentred-sal01-qa.co.uk/#/dashboard"
       |        }
       |      ]
       |    }, {
       |      "name" : "Production",
       |      "services" : [
       |        {
       |          "name": "jenkins",
       |          "displayName": "Jenkins",
       |          "url": "https://deploy-prod.co.uk/job/deploy-microservice"
       |        }, {
       |          "name": "grafana",
       |          "displayName": "Grafana",
       |          "url": "https://grafana-prod.co.uk/#/dashboard"
       |        }
       |      ]
       |    }
       |  ]
       |}
    """.stripMargin

  val serviceJenkinsData: String =
    """
      |{
      |   "service": "service-1",
      |   "jenkinsURL": "http://jenkins/service-1/"
      |}
      |""".stripMargin

  val teamDetailsData =
    """
      |{
      |  "name" : "teamA",
      |  "repos": {
      |    "Service": [
      |      "service1",
      |      "service2"
      |    ],
      |    "Library": [
      |      "library1",
      |      "library2"
      |    ],
      |    "Other": [
      |      "other1",
      |      "other2"
      |    ]
      |  },
      |  "ownedRepos": []
      |}
    """.stripMargin

  val teamsAndRepositories =
    s""" [$teamDetailsData] """

  val prototypeDetailsData =
    s"""
       |{
       |  "name": "2fa-prototype",
       |  "isPrivate": false,
       |  "isArchived": false,
       |  "description": "some description",
       |  "createdAt": ${createdAt.epochMillis},
       |  "lastActive": ${lastActiveAt.epochMillis},
       |  "repoType": "Prototype",
       |  "owningTeams": [
       |    "The True Owners"
       |  ],
       |  "teamNames": [
       |    "CATO",
       |    "Designers"
       |  ],
       |  "githubUrl": {
       |    "name": "github",
       |    "displayName": "Github.com",
       |    "url": "https://github.com/HMRC/2fa-prototype"
       |  },
       |  "environments": [ ]
       |}
    """.stripMargin

  val libraryDetailsData =
    """
      |{
      |	 "name": "serv",
      |  "isPrivate": false,
      |  "isArchived": false,
      |  "description": "some description",
      |  "createdAt": 1456326530000,
      |  "lastActive": 1478602555000,
      |  "repoType": "Library",
      |  "owningTeams": [
      |    "The True Owners"
      |  ],
      |  "teamNames": ["teamA", "teamB"],
      |	 "githubUrl": {
      |    "name": "github",
      |    "displayName": "github.com",
      |    "url": "https://github.com/hmrc/serv"
      |	 }
      |}
    """.stripMargin

  val deploymentThroughputData =
    """[
      |  {
      |    "period": "2015-11",
      |    "from": "2015-12-01",
      |    "to": "2016-02-29",
      |    "throughput": {
      |      "leadTime": {
      |        "median": 6
      |      },
      |      "interval": {
      |        "median": 1
      |      }
      |    },
      |    "stability": {
      |      "hotfixRate": 23,
      |      "hotfixLeadTime": {
      |        "median": 6
      |      }
      |    }
      |  },
      |  {
      |    "period": "2015-12",
      |    "from": "2015-12-01",
      |    "to": "2016-02-29",
      |    "throughput": {
      |      "leadTime": {
      |        "median": 6
      |      },
      |      "interval": {
      |        "median": 5
      |      }
      |    },
      |    "stability": {
      |      "hotfixRate": 23,
      |      "hotfixLeadTime": {
      |        "median": 6
      |      }
      |    }
      |  },
      |  {
      |    "period": "2016-01",
      |    "from": "2015-12-01",
      |    "to": "2016-02-29",
      |    "throughput": {
      |      "leadTime": {
      |        "median": 6
      |      },
      |      "interval": {
      |        "median": 6
      |      }
      |    }
      |  }
      |]
      | """.stripMargin

  val jobExecutionTimeData =
    """[
      |  {
      |    "period":"2016-05",
      |    "from":"2016-03-01",
      |    "to":"2016-05-31"
      |  },
      |  {
      |    "period":"2016-06",
      |    "from":"2016-04-01",
      |    "to":"2016-06-30",
      |    "duration":{
      |      "median":623142
      |    }
      |  },
      |  {
      |    "period":"2016-07",
      |    "from":"2016-05-01",
      |    "to":"2016-07-31",
      |    "duration":{
      |      "median":632529
      |    }
      |  },
      |  {
      |    "period":"2016-08",
      |    "from":"2016-06-01",
      |    "to":"2016-08-31",
      |    "duration":{
      |      "median":632529
      |    }
      |  },
      |  {
      |    "period":"2016-09",
      |    "from":"2016-07-01",
      |    "to":"2016-09-30",
      |    "duration":{
      |      "median":637234
      |    }
      |  },
      |  {
      |    "period":"2016-10",
      |    "from":"2016-08-01",
      |    "to":"2016-10-31"
      |  },
      |  {
      |    "period":"2016-11",
      |    "from":"2016-09-01",
      |    "to":"2016-11-30"
      |  },
      |  {
      |    "period":"2016-12",
      |    "from":"2016-10-01",
      |    "to":"2016-12-31"
      |  },
      |  {
      |    "period":"2017-01",
      |    "from":"2016-11-01",
      |    "to":"2017-01-31"
      |  },
      |  {
      |    "period":"2017-02",
      |    "from":"2016-12-01",
      |    "to":"2017-02-28"
      |  },
      |  {
      |    "period":"2017-03",
      |    "from":"2017-01-01",
      |    "to":"2017-03-31"
      |  },
      |  {
      |    "period":"2017-04",
      |    "from":"2017-02-01",
      |    "to":"2017-04-25",
      |    "duration":{
      |      "median":645861
      |    }
      |  }
      |]""".stripMargin

  val digitalServiceData =
    """
      |{
      |  "name": "service-1",
      |  "lastUpdatedAt": 1494240869000,
      |  "repositories": [
      |    {
      |      "name": "catalogue-frontend",
      |      "createdAt": "2016-02-24T15:08:50",
      |      "lastUpdatedAt": "2017-05-08T10:53:29",
      |      "repoType": "Service",
      |      "teamNames": ["Team1", "Team2"]
      |    },
      |    {
      |      "name": "repository-jobs",
      |      "createdAt": "2017-04-11T13:14:29",
      |      "lastUpdatedAt": "2017-05-08T10:54:29",
      |      "repoType": "Service",
      |      "teamNames": ["Team1"]
      |    },
      |    {
      |      "name": "teams-and-repositories",
      |      "createdAt": "2016-02-05T10:55:16",
      |      "lastUpdatedAt": "2017-05-08T10:53:58",
      |      "repoType": "Service",
      |      "teamNames": ["Team2"]
      |    }
      |  ]
      |}
    """.stripMargin

  val configServiceEmpty =
    """[
      |]
    """.stripMargin

  val configServiceService1 =
    """[
      |  {
      |    "environment": "qa",
      |    "routes": [
      |      {
      |        "frontendPath": "/test/qa/ccc",
      |        "backendPath": "https://test.co/ccc",
      |        "ruleConfigurationUrl": "https://github.com/hmrc/mdtp-frontend-routes/blob/master/production/frontend-proxy-application-rules.conf#L29",
      |        "isRegex": false
      |      }
      |    ]
      |  },
      |  {
      |    "environment": "production",
      |    "routes": [
      |      {
      |        "frontendPath": "/test/prod/ccc",
      |        "backendPath": "https://test.co/prod/ccc",
      |        "ruleConfigurationUrl": "https://github.com/hmrc/mdtp-frontend-routes/blob/master/production/frontend-proxy-application-rules.conf#L29",
      |        "isRegex": false
      |      }
      |    ]
      |  },
      |  {
      |    "environment": "development",
      |    "routes": [
      |      {
      |        "frontendPath": "/test/dev/ccc",
      |        "backendPath": "https://test.co/ccc",
      |        "ruleConfigurationUrl": "https://github.com/hmrc/mdtp-frontend-routes/blob/master/production/frontend-proxy-application-rules.conf#L29",
      |        "isRegex": false
      |      }
      |    ]
      |  }
      |]
    """.stripMargin


  val dependenciesWithRuleViolation =
  """{
   "repositoryName" : "catalogue-frontend",
   "lastUpdated" : "2018-12-14T15:45:07.335Z",
   "sbtPluginsDependencies" : [
      {
         "currentVersion" : {
            "minor" : 1,
            "original" : "1.1.0",
            "major" : 1,
            "patch" : 0
         },
         "isExternal" : false,
         "latestVersion" : {
            "minor" : 1,
            "original" : "1.1.0",
            "patch" : 0,
            "major" : 1
         },
         "name" : "sbt-distributables",
         "bobbyRuleViolations" : []
      },
      {
         "isExternal" : false,
         "latestVersion" : {
            "patch" : 0,
            "major" : 1,
            "minor" : 13,
            "original" : "1.13.0"
         },
         "name" : "sbt-auto-build",
         "currentVersion" : {
            "original" : "1.13.0",
            "minor" : 13,
            "major" : 1,
            "patch" : 0
         },
         "bobbyRuleViolations" : [
            {
               "range" : {
                  "range" : "(,1.4.0)",
                  "upperBound" : {
                     "version" : {
                        "original" : "1.4.0",
                        "minor" : 4,
                        "patch" : 0,
                        "major" : 1
                     },
                     "inclusive" : false
                  }
               },
               "reason" : "Play 2.5 upgrade",
               "from" : "2017-05-01"
            }
         ]
      }
   ],
   "otherDependencies" : [
      {
         "isExternal" : false,
         "latestVersion" : {
            "minor" : 13,
            "original" : "0.13.17",
            "major" : 0,
            "patch" : 17
         },
         "currentVersion" : {
            "patch" : 17,
            "major" : 0,
            "minor" : 13,
            "original" : "0.13.17"
         },
         "name" : "sbt",
         "bobbyRuleViolations" : []
      }
   ],
   "libraryDependencies" : [
      {
         "latestVersion" : {
            "minor" : 2,
            "original" : "3.2.0",
            "major" : 3,
            "patch" : 0
         },
         "isExternal" : false,
         "currentVersion" : {
            "major" : 3,
            "patch" : 0,
            "original" : "3.0.0",
            "minor" : 0
         },
         "name" : "hmrctest",
         "bobbyRuleViolations" : []
      },
      {
         "name" : "simple-reactivemongo",
         "isExternal" : false,
         "latestVersion" : {
            "patch" : 0,
            "major" : 7,
            "minor" : 3,
            "original" : "7.3.0-play-26"
         },
         "currentVersion" : {
            "major" : 7,
            "patch" : 0,
            "original" : "7.0.0-play-26",
            "minor" : 0
         },
         "bobbyRuleViolations" : [
            {
               "range" : {
                  "lowerBound" : {
                     "inclusive" : true,
                     "version" : {
                        "original" : "7.0.0",
                        "minor" : 0,
                        "major" : 7,
                        "patch" : 0
                     }
                  },
                  "upperBound" : {
                     "inclusive" : true,
                     "version" : {
                        "minor" : 7,
                        "original" : "7.7.0",
                        "major" : 7,
                        "patch" : 0
                     }
                  },
                  "range" : "[7.0.0,7.7.0]"
               },
               "reason" : "Uses ReactiveMongo [0.15.0, 0.16.0] which has problems with reconnecting",
               "from" : "2019-02-06"
            }
         ]
      }
   ]
}
"""

  val serviceData: String =
    """{
      |  "name": "serv",
      |  "isPrivate": false,
      |  "isArchived": false,
      |  "repoType": "Service",
      |  "owningTeams": [ "The True Owners" ],
      |  "teamNames": ["teamA", "teamB"],
      |  "description": "some description",
      |  "createdAt": 1456326530000,
      |  "lastActive": 1478602555000,
      |  "githubUrl": {
      |    "name": "github",
      |    "displayName": "github.com",
      |    "url": "https://github.com/hmrc/serv"
      |  },
      |  "environments" : [
      |    {
      |      "name" : "Production",
      |      "services" : [
      |        {
      |          "name": "ser1",
      |          "displayName": "service1",
      |          "url": "http://ser1/serv"
      |        }, {
      |          "name": "ser2",
      |          "displayName": "service2",
      |          "url": "http://ser2/serv"
      |        }
      |      ]
      |    }, {
      |      "name" : "Staging",
      |      "services" : [
      |        {
      |          "name": "ser1",
      |          "displayName": "service1",
      |          "url": "http://ser1/serv"
      |        }, {
      |          "name": "ser2",
      |          "displayName": "service2",
      |          "url": "http://ser2/serv"
      |        }
      |      ]
      |    }
      |  ]
      |}
    """.stripMargin

  val jenkinsData: String =
    """{
      |  "service": "lib",
      |  "jenkinsURL": "http://jenkins/lib/"
      |}
      |""".stripMargin

  val libraryData: String =
    """{
      |  "name": "lib",
      |  "isPrivate": false,
      |  "isArchived": false,
      |  "description": "some description",
      |  "createdAt": 1456326530000,
      |  "lastActive": 1478602555000,
      |  "repoType": "Library",
      |  "owningTeams": [ "The True Owners" ],
      |  "teamNames": ["teamA", "teamB"],
      |  "githubUrl": {
      |    "name": "github",
      |    "displayName": "github.com",
      |    "url": "https://github.com/hmrc/lib"
      |  }
      |}
    """.stripMargin

  val indicatorData: String =
    """[
      |  {
      |    "period":"2015-11",
      |    "leadTime":{
      |      "median":6
      |    },
      |    "interval":{
      |      "median":1
      |    }
      |  },
      |  {
      |    "period":"2015-12",
      |    "leadTime":{
      |      "median":6
      |    },
      |    "interval":{
      |      "median":5
      |    }
      |  },
      |  {
      |    "period":"2016-01",
      |    "leadTime":{
      |      "median":6
      |    },
      |    "interval":{
      |      "median":6
      |    }
      |  }
      |]
    """.stripMargin

  def shutterApiData(shutterType: ShutterType, env: Environment, status: ShutterStatusValue) =
    s"""
      {
        "name": "serv",
        "type": "${shutterType.asString}",
        "environment": "${env.asString}",
        "status": {
           "value": "${status.asString}",
           "useDefaultOutagePage": false
        }
      }
    """

  val profiles: String =
    """
    |[
    |
    |]
    """".stripMargin

  val serviceDependenciesData: String =
    """{
      |  "uri": "/",
      |  "name": "service-name",
      |  "version": "1.0.0",
      |  "runnerVersion": "1.0.0",
      |  "java": {
      |    "version": "1.0.0",
      |    "vendor": "openjdk",
      |    "kind": ""
      |  },
      |  "classpath": "",
      |  "dependencies": []
      }""".stripMargin
}
