# my-closet

## Instructions for launching the application:
XAMPP – start Apache and MySQL (check port, 3306 is set in the app) – run script if needed.
Backend – my-closet project – lein run
Frontend – my-closet-frontend project – npx shadow-cljs watch app

## Motivation
Idea for this project came out of nowhere, but after all I'm just a girl. I heard there are similar applications that allow you to insert your clothes and then you can make combinations and plan outfits anywhere. I wanted to make next step, so I made application that generates combinations based on your clothing items. 

## About
There are rules (now basic) that are calculated when generating combinations. I also added an algorithm that learns from users' feedback. If user1 and user2 like the same combination, each of them will receive some combinations that the other one liked. It's like they have similar taste, so combinations from the other users are being recommended. If there are more "compatibile" users, weights are being calculated, in the other words, we are searching for the most similar taste. If user has no compatibility with others, he will receive combinations generated in the app considering application logic. Rules used for that are for now basic, but in the future professional stylist could be consulted.

The app is now designed to be used by one household, but model can change. We don't care how clothes are inserted into database, for all we know, they can be scanned automatically in smart closet. Users are also added in advance, in app you can switch accounts, but there are no classic login. The idea was to make using application super simple, without unnecessary parts.

## Technologies and libraries
Backend is written in Clojure, frontend in ClojureScript. MySQL is used as Database Management System. 
Libraries used for this app:
### Core language
•	org.clojure/clojure – The main Clojure library (version 1.11.1), the language core.
### Testing
•	midje – A testing framework for Clojure, focused on readable, behavior-driven development (BDD) style tests.
### Database
•	seancorfield/next.jdbc – A modern JDBC wrapper for Clojure, simplifying interaction with relational databases.
•	mysql/mysql-connector-java – A Java driver for connecting to MySQL/MariaDB databases.
### Web server and HTTP layer
•	ring/ring-core – The core library for building web applications in Clojure; defines the HTTP request/response model.
•	ring/ring-json – Middleware for parsing and generating JSON in Ring applications.
•	ring/ring-jetty-adapter – An adapter for running Ring applications on the Jetty web server.
•	ring-cors – Middleware for adding CORS (Cross-Origin Resource Sharing) support.
### Routing and content negotiation
•	metosin/reitit – A fast and flexible routing framework for Clojure/ClojureScript.
•	metosin/muuntaja – A library for automatic encoding and decoding of data (JSON, Transit, EDN, etc.).
### JSON processing
•	cheshire – A fast JSON encoding/decoding library for Clojure.
### UI and State Management
•	reagent – A minimalistic ClojureScript interface to React, allowing you to build reactive UI components.
•	re-frame – A ClojureScript framework for building SPAs using Reagent, with a functional, event-driven architecture.
### Debugging and Development Tools
•	day8.re-frame/tracing – Provides tracing and instrumentation support for debugging re-frame applications.
•	binaryage/devtools – Enhances ClojureScript’s integration with browser developer tools for easier debugging.
•	day8.re-frame/re-frame-10x – A powerful time-travel and debugging tool for re-frame applications.
### HTTP and Effects
•	day8.re-frame/http-fx – A re-frame effects handler for making HTTP requests using cljs-ajax.
•	cljs-http – A simple and asynchronous HTTP client for ClojureScript.
•	ajax.core – A ClojureScript library for making asynchronous HTTP requests (supports EDN, JSON, Transit, etc.).
•	ajax.edn – Part of the cljs-ajax suite, providing EDN request/response handling.
### Routing
•	bidi – A bidirectional routing library for Clojure and ClojureScript, enabling both route matching and URL generation.
•	clj-commons/pushy – A library for client-side navigation using HTML5 pushState without reloading the page.
### Core Utilities
•	org.clojure/tools.reader – A library for reading Clojure and ClojureScript code/data, with customizable reader behavior.
•	cljs.tools.reader – ClojureScript version of the reader, used for parsing EDN and Clojure data structures.
•	clojure.string – Core library for string manipulation in Clojure/ClojureScript.
### Asynchronous Processing
•	cljs.core.async – A library for asynchronous programming in ClojureScript using channels and goroutines-like constructs.
### Backend Database
•	MySQL – Relational database management system used as the main data store, accessed through JDBC and next.jdbc in the backend.


FIXME: description

## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar my-closet-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2024 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
