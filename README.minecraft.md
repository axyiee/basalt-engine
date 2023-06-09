## Roadmap

- [ ] Entity composition
- [ ] Platform-agnostic object and event abstraction
- [ ] Synchronous (main-thread) code execution
- [ ] Input/output support in queries with FS2
- [ ] Advanced entity querying
- [ ] State/scene preservation and loading
- [ ] Query composition with entities and streams
- [ ] Persistent Data Container (PDC) support on Paper
- [ ] Data persistence and (de)serialization to Circe
- [ ] Network pipeline injection/transformation
- [ ] Server-side data pack replication
- - [ ] Resource pack integration
- - [ ] HUD and font rendering
- - [ ] Custom blocks and items
- [ ] Multi-platform Scala support
- [ ] Minecraft: Java Edition protocol implementation
- [ ] Not confirmed: Server-side Fabric toolchain support
- [ ] Not confirmed: The Kotlin Programming Language support
- [ ] Not confirmed: Bedrock Edition support

## Background

Minecraft does **not** have a server extension standard through all of its modding toolchains and server
implementations. Basalt strives to solve this problem by allowing users to follow the "write once, run
everywhere" principle.

Basalt is built on top of the [Scala 3] programming language, which is a modern, general-purpose
multi-paradigm programming language. Since the Kotlin programming language has a really huge market
value, Basalt will also try to support it as possible, although it wouldn't as prioritized as Scala
is.

Basalt strives to be as modular, clean, performant, and built on top of modern mainstream and solid
technologies as possible, such as the [Typelevel ecosystem] and [FS2], allowing interchangeability
between Minecraft and non-Minecraft environments easily.

Server-only features you could see on data packs is planned to be supported as well! This includes, but
not limited to, resource packs, custom blocks and items, HUD rendering, biomes, world generation, etc.

Optimization and data saving is also a big concern for Basalt, for that specific reason, we decided to follow up with the following design choice: attributes are data attached to an entity or object by default,
such as health, position, etc. Those are treated as components but aren't serialized unless specified otherwise.

[Typelevel ecosystem]: https://typelevel.org
[FS2]: https://fs2.io
[Bevy]: https://bevyengine.org
[Bevy Engine]: https://bevyengine.org
[Scala 3]: https://scala-lang.org
[Cats Effect]: https://typelevel.org/cats-effect/

## Example

**Yet to be implemented. Take this as a proof of concept.**

```scala
import basalt.core.{Extension, Component}
import basalt.core.event.PlayerJoin
import basalt.core.protocol.java.JavaProtocol
import basalt.core.syntax.java._
import basalt.core.syntax._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.TimeUnit

def extension[F[_]: Sync: Clock]: Extension[F] =
  Extension[F](JavaProtocol.compat(from = v"1.19.0", to = v"1.19.4"))
    .metadata(namespace = "my-welcoming-extension", version = "0.0.0-SNAPSHOT")
    .withSystems(firstJoinWelcome[F])

case class FirstJoin(timestamp: FiniteDuration) extends Component

def firstJoinWelcome[F[_]: Clock] =
  System[F]
    .named[EventRead[PlayerJoin] |: Not[FirstJoin] |: Fin]("first-join-welcome") { ctx =>
        val event |: _ = ctx
        Clock[F].realTime(TimeUnit.MILLISECONDS)
            >> (event.assignee.add(FirstJoin(_)))
            >> (event.assignee.sendRaw("{\"text\":\"Hello!\"}"))
    }
```

## Limitations

- The build system here has to be Gradle due to better compatibility with the Fabric mod loader and Paperweight Gradle plugin
