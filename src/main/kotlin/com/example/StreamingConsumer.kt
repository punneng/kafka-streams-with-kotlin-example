package com.example

import com.fasterxml.jackson.databind.JsonNode
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.connect.json.JsonSerializer
import org.apache.kafka.connect.json.JsonDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.Consumed
import org.apache.kafka.streams.kstream.KStream

import java.util.Properties

fun main(args: Array<String>) {
    val bootstrapServersConfig = System.getenv("BOOTSTRAP_SERVERS_CONFIG") ?: "127.0.0.1:9092"
    val applicationIdConfig = System.getenv("APPLICATION_ID_CONFIG") ?: "kafka-streams-with-kotlin-example"
    val props = Properties()

    with(props) {
        put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServersConfig)
        put(StreamsConfig.APPLICATION_ID_CONFIG, applicationIdConfig)
        put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().javaClass.name)
        put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().javaClass.name)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    }

    // json Serde
    val jsonSerializer = JsonSerializer()
    val jsonDeserializer = JsonDeserializer()
    val jsonSerde = Serdes.serdeFrom<JsonNode>(jsonSerializer, jsonDeserializer)

    val names = mutableListOf<String>()
    val builder = StreamsBuilder()

    val nodeStreams: KStream<String, JsonNode> = builder.stream("stream-topic", Consumed.with(Serdes.String(), jsonSerde))
    nodeStreams.mapValues {
        println("VALUE: $it")
    }

    val streams = KafkaStreams(builder.build(), props)
    streams.cleanUp()
    streams.start()

    streams.localThreadsMetadata().forEach { data -> println(data) }

    // NOTE: to shutdown hook to correctly close the streams application
    Runtime.getRuntime().addShutdownHook(Thread(streams::close))
}