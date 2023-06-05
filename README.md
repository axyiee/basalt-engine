# Basalt Engine

An ECS[¹](#reference) engine built from ground up in [Scala 3] with architectural
support for [Cats Effect] and modularity in mind.

[Scala 3]: https://scala-lang.org
[Cats Effect]: https://typelevel.org/cats-effect/

## Roadmap

- [ ] Entity composition
- [ ] Platform-agnostic object and event abstraction
- [ ] Synchronous (main-thread) code execution
- [ ] Input/output support in queries with FS2
- [ ] State/scene preservation and loading
- [ ] Query composition with entities and streams
- [ ] Data persistence and (de)serialization to Circe

## Background

Inspired by [Bevy Engine] – a data-driven game engine written in Rust – ECS implementation, we focus on
logic built on top of resources, entities, and systems (could be simplified as data and functions).
This approach is really nice because it's a very flexible and powerful design pattern that allows
code to be easily reused, extended, and quickly written.

[Bevy Engine]: https://bevyengine.org

## Reference

**¹:** Software architectural pattern "Entity component system".

## Related projects

- [ECScala](https://github.com/atedeg/ecscala) - An ECS Scala framework. This is such a nice proof-of-concept that
  helped this project to get started, so I'd like to thank its developers!
- [flecs](https://github.com/SanderMertens/flecs) - A fast entity component system (ECS) for C & C++
- [Dominion](https://github.com/dominion-dev/dominion-ecs-java) - Insanely fast ECS (Entity Component System) for Java
