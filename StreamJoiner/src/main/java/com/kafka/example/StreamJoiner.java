package com.kafka.example;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class StreamJoiner {

  private static Properties config = new Properties();

  public static void main(String[] args) {
    initializeConfigs();

    StreamsBuilder builder = new StreamsBuilder();
    // 1 - stream from Kafka
    KStream<String, String> left = builder.stream("table1");
    KStream<String, String> right = builder.stream("table2");
    left.join(
            right,
            (leftValue, rightValue) ->
                "left=" + leftValue + ", right=" + rightValue, /* ValueJoiner */
            JoinWindows.of(TimeUnit.MINUTES.toMillis(5)),
            Joined.with(
                Serdes.String(), /* key */
                Serdes.String(), /* left value */
                Serdes.String()) /* right value */)
        .to("joined-output");
    KafkaStreams streams = new KafkaStreams(builder.build(), config);
    streams.start();

    // shutdown hook to correctly close the streams application
    Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

    // Update:
    // print the topology every 10 seconds for learning purposes
    while (true) {
      System.out.println(streams.toString());
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  private static void initializeConfigs() {
    config.put(StreamsConfig.APPLICATION_ID_CONFIG, "stream-joiner-app");
    config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
  }
}
