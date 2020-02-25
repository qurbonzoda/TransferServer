## RESTful server for transactions between accounts 

This is a server with REST API that handles money transfers.

There are 4 main entities in the system:

* User: Person whose accounts you serve. Every user has zero or more accounts. \
You can open a new account for the user choosing account's currency. \
To delete a user firstly all his/her accounts must be deleted. \
See: https://github.com/qurbonzoda/TransferServer/blob/master/HttpRequests/Users.http

* Account: Every account belongs to a user and has a currency associated with it. \
You can deposit some amount of money into account, or withdraw from it. \
To delete an account all money in it's balance must get withdrawn. \
See: https://github.com/qurbonzoda/TransferServer/blob/master/HttpRequests/Accounts.http

* Currency: Each currency has its valuation (exchange rate). \
It is allowed to deposit some USD into an account associated with EUR currency. \
In that case dollars will be converted to EUR using currency's current valuation. \
You can also transfer money between accounts with different currencies. \
Also, there is an endpoint provided to change currency rates. \
See: https://github.com/qurbonzoda/TransferServer/blob/master/HttpRequests/Currencies.http

* Transfer: Transfer objects are immutable. There is no endpoint provided to mutate transfers. \
However you can create new transfers, get particular transaction by `id`, \
or get all transactions where a particular account was involved. \
See: https://github.com/qurbonzoda/TransferServer/blob/master/HttpRequests/Transfers.http


### Frameworks used:

* [ktor](https://ktor.io) with netty engine.
* `logback` used for logging.
* [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) for serializing kotlin objects to json and vise versa.
* [kotlinx.atomicfu](https://github.com/Kotlin/kotlinx.atomicfu) and [kotlinx-collections-immutable](https://github.com/Kotlin/kotlinx.collections.immutable) 
to implement fast and thread-safe requests handlers.


### Build & Run

To start the server in `http://127.0.0.1:8080` run `./gradlew run`. \
To build fat jar run `./gradlew jar`,  \
`java -jar build/libs/TransferServer-1.0-SNAPSHOT.jar` to run the jar file.
