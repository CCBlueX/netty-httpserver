# Netty Http Server

Netty HttpServer is a Kotlin-based library for building web REST APIs on top of Netty. It provides a simple way to handle HTTP requests and responses, with built-in support for WebSockets and file serving. This library has been used in production as part of the [LiquidBounce](https://github.com/CCBlueX/LiquidBounce) project and is now available as a standalone library.

## Getting Started

### Installation

To include Netty HttpServer in your project, add the following dependency to your `build.gradle` file:

```gradle
implementation 'com.github.CCBlueX:netty-httpserver:2.0.0'
```

### Basic Usage

Here is an example of how to use the library to create a simple "Hello, World!" REST API:

```kotlin
import com.google.gson.JsonObject
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.util.httpOk

fun main() {
    val server = HttpServer()

    server.routeController.apply {
        get("/hello") {
            httpOk(JsonObject().apply {
                addProperty("message", "Hello, World!")
            })
        }
    }

    server.start(8080)  // Start the server on port 8080
}
```

In this example, the server listens on port `8080` and responds with a JSON message `"Hello, World!"` when accessing the `/hello` endpoint.

### Examples

You can find additional examples in the `/examples` folder of the repository. These include:

1. **Hello World Example**: A basic server that responds with "Hello, World!".
2. **Echo Server**: A server that echoes back any JSON data sent to it.
3. **File Server**: A server that serves files from a specified directory.

### Running the Examples

To run the examples, you can use Gradle. In the root of the repository, execute the following command:

```bash
./gradlew run -Pexample=<example-name>
```

Replace `<example-name>` with the name of the example you want to run, such as `hello-world`, `echo-server`, or `file-server`.

For instance, to run the Hello World example, use:

```bash
./gradlew run -Pexample=hello-world
```

## License

Netty HttpServer is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for more details.

## Contributing

Contributions are welcome! If you have suggestions or improvements, please open an issue or submit a pull request.

## Author

Netty HttpServer is developed and maintained by CCBlueX. It was originally part of the LiquidBounce project.

---

Feel free to explore the examples provided and adapt them to your specific needs. Happy coding!