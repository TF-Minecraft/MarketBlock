# MarketBlock

> A market for gathered goods, with prices that respond to demand.

MarketBlock gives TF-Minecraft players a place to sell resources for denars. Its market interface groups goods into familiar categories, shows the current offer, and turns gathered materials into income through DenarEconomy.

Prices respond to activity: selling a good lowers its demand, while demand recovers over time. Food condition also matters, with fresh, stale, and rotten goods receiving different returns. That makes the choice of what to sell, and when, part of the trading experience.

## Features

- **Browse by category** — find offers for wood, farm produce, metals, minerals, stone, alchemy supplies, and fish.
- **Demand-based prices** — see offers change as players sell goods and market demand rebuilds.
- **Freshness-aware sales** — account for food condition when calculating the payment, including mixed-condition batches.
- **Clear sale feedback** — show the amount sold, denars received, and any freshness adjustments.
- **Custom goods support** — trade both ordinary Minecraft resources and supported custom items through the same market.

## In the world

MarketBlock is the server market counterpart to player-run shops: a dedicated market block provides the selling interface, while the shared denar economy handles the earnings.

## Documentation

[Project documentation](https://github.com/TF-Minecraft/Docs/blob/main/projects/MarketBlock/README.md)

Technical documentation is maintained in [TF-Minecraft/Docs](https://github.com/TF-Minecraft/Docs).

## Tests

Install the pinned shared plugin dependencies, then run the build with Java 21:

```sh
python3 path/to/TLibs/tools/install-plugins.py --pom pom.xml --mode pinned
mvn clean verify
```

Point the installer at your TLibs checkout.

Tests use JUnit, MockBukkit, and Mockito without a live Minecraft server. They
cover trade creation, purchases, menus, persistence, demand recovery, and the
plugin lifecycle, including regressions for invalid quantities and stale state.
`mvn clean verify` enforces 100% runtime line coverage with no production-code
exclusions. JaCoCo's HTML and XML reports are written to `target/site/jacoco/`.

## License

Copyright (c) 2026 TF-Minecraft contributors.

TF-Minecraft-authored material in this repository is licensed under the
[Artistic License 2.0](LICENSE). Third-party dependencies and bundled material
retain their own licenses.
