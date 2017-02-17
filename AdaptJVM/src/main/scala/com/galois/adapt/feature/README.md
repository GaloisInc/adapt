This directory stores feature extractors. These are materialized as actors that extend
`SubscriptionActor`.

### `SubscriptionActor`

A subscription actor is an actor that has a more specialized notion of sending and receiving messages

  * it informs other subscription actors downstream that it is interested in their outputs
  * broadcasts its own output to all subscription actors upstream that are subscribed to it

For example, an anomaly detector subscription actor may "subscribe" to the output of several feature
extractor subscription actors.

### What goes into a `SubscriptionActor`?

Every subscription actor corresponds to a class that extends `SubscriptionActor`. These will look
roughly like

```scala
import akka.actor._

class SomeFeature( /* some class arguments */ )
  extends SubscriptionActor[/* type of messages consumed */, /* type of messages produced */]] {

  // The messages this feature actor wishes to receive
  val subscriptions: Set[Subscription[/* type of messages consumed */]] = Set(
    ...
    // Each subscription consists of an `ActorRef` and a packaging function. The latter returns an
    // 'Option' of the type of messages we expect to consume. 'None' would indicate that we aren't
    // interested in a particular message.
    Subscription( /* 'ActorRef' */, /* function to package whatever is the output of that actor */)
    ...
  )

  // This must come immediately after the subscriptions - it sends the subscriptions to the actors
  // upstream
  initialize()

  // What to do with a message. This is where calls to 'broadCast' (to send to actors downstream)
  // should be made.
  def process(msg: /* type of message consumed */): Unit = ...
}
```

### Examples

All classes in this directory are examples of feature extractors implemented using subscription
actors.
