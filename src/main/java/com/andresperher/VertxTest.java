package com.andresperher;

import io.vertx.rxjava3.SingleHelper;
import io.vertx.rxjava3.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import com.andresperher.verticles.MainVerticle;

@Slf4j
public class VertxTest {

	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();

		vertx.rxDeployVerticle(MainVerticle.create(8080))
			.doOnSuccess(id -> log.info("Server up with id: {}", id))
			.doOnError(err -> log.error("Server failed: {}", err.getMessage()))
			.subscribe(SingleHelper.nullObserver());
	}

}
