# Bridge Fit

**Bridge Fit** is a personal fork of [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge) with a new
interface inspired by Apple Fitness: activity rings, a Summary of the day and clean detail screens for heart rate,
sleep, SpO₂, HRV, stress and skin temperature. It is written in Jetpack Compose on top of the full Gadgetbridge app.

All device support, Bluetooth communication, sync, the database and Health Connect export come from Gadgetbridge, which
is cloudless and runs entirely on your phone. Bridge Fit regularly merges upstream Gadgetbridge to keep getting its
improvements and new devices.

The main target is the Colmi R09 smart ring. A gesture control feature for the ring is planned.

- Product vision: [`docs/vision.pt-BR.md`](../docs/vision.pt-BR.md) (Portuguese)
- How the fork is organised and how to contribute, for humans and AI agents: [`AGENTS.md`](../AGENTS.md)
- The original Gadgetbridge README: [`README.md`](../README.md)

## Building

```sh
./gradlew :app:assembleBridgefitDebug
```

Requires JDK 21 and the Android SDK. The `bridgefit` flavor installs as `com.apenasangelo.bridgefit`, next to an
existing Gadgetbridge.

## License

Bridge Fit is distributed under the [GNU Affero General Public License v3.0](../LICENSE), like Gadgetbridge.
All credit for the Gadgetbridge base goes to the [Gadgetbridge contributors](../CONTRIBUTORS.rst).
Bridge Fit is not affiliated with Apple, and it uses no Apple assets.
