package com.thehecklers.thing2;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

@SpringBootApplication
public class Thing2Application {

	public static void main(String[] args) {
		SpringApplication.run(Thing2Application.class, args);
	}

	@Bean
	RSocketRequester requester(RSocketRequester.Builder builder) {
//		return builder.tcp("localhost", 9091);
		return builder.websocket(URI.create("http://localhost:9091"));
	}
}

@Component
@AllArgsConstructor
class Thing2Component {
	private final RSocketRequester requester;
	private final StreamBridge streamBridge;

//	@PostConstruct
	void reqStream() {
		requester.route("reqstream")
				.data(Instant.now())
				.retrieveFlux(Aircraft.class)
				.subscribe(ac -> System.out.println("🛩 " + ac));
	}

	@PostConstruct
	void channel() {
		List<String> obsList = List.of("SKC, VIS 10SM",
				"BKN 090, VIS 8SM",
				"OVC 03, VIS 1/2SM");
		Random rnd = new Random();

		requester.route("channel")
				.data(Flux.interval(Duration.ofSeconds(1))
						.map(l -> new Weather(Instant.now(),
								obsList.get(rnd.nextInt(obsList.size())))))
				.retrieveFlux(Aircraft.class)
				.doOnNext(ac -> streamBridge.send("sendAC-out-0", ac))
				.subscribe(ac -> System.out.println("✈️ " + ac));
	}
}


@Data
@AllArgsConstructor
class Weather {
	private Instant when;
	private String observation;
}

@Data
class Aircraft {
	private String callsign, reg, flightno, type;
	private int altitude, heading, speed;
	private double lat, lon;
}