package com.andresperher.verticles;

import io.reactivex.rxjava3.core.Completable;
import io.vertx.rxjava3.CompletableHelper;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.ext.web.Router;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.ACCEPTED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@RequiredArgsConstructor(staticName = "create")
public class MainVerticle extends AbstractVerticle {

	private static final int CHUNK_MB_SIZE = 100;
	private static final List<byte[]> memoryChunks = new ArrayList<>();

	private final int port;

	@Override
	public Completable rxStart() {
		var router = Router.router(vertx);

		registerAllocationRoute(router);
		registerDeallocationRoute(router);
		registerDeallocationAllRoute(router);
		registerGCRoute(router);

		return vertx.createHttpServer()
			.requestHandler(router)
			.rxListen(port)
			.ignoreElement();
	}

	private void registerAllocationRoute(Router router) {
		router.post("/allocate")
			.handler(ctx -> Completable.fromAction(() -> {
					var chunk = new byte[CHUNK_MB_SIZE * 1024 * 1024]; // Allocate 100 MB
					memoryChunks.add(chunk);
					log.debug("New chunk allocated. Total memory allocated: {}MB", memoryChunks.size() * CHUNK_MB_SIZE);
				})
				.doOnComplete(() -> ctx.response().setStatusCode(ACCEPTED.code()).end())
				.doOnError(err -> ctx.response().setStatusCode(INTERNAL_SERVER_ERROR.code()).end(err.getMessage()))
				.subscribe(CompletableHelper.nullObserver()));
	}

	private void registerDeallocationRoute(Router router) {
		router.post("/release")
			.handler(ctx -> Completable.fromAction(() -> {
					if (memoryChunks.isEmpty()) {
						log.warn("No memory chunks to deallocate");
						return;
					}
					memoryChunks.removeLast();
					log.debug("Chunk released -> Total memory allocated: {} MB", memoryChunks.size() * CHUNK_MB_SIZE);
				})
				.doOnComplete(() -> ctx.response().setStatusCode(ACCEPTED.code()).end())
				.doOnError(err -> ctx.response().setStatusCode(INTERNAL_SERVER_ERROR.code()).end(err.getMessage()))
				.subscribe(CompletableHelper.nullObserver()));
	}

	private void registerDeallocationAllRoute(Router router) {
		router.post("/release-all")
			.handler(ctx -> Completable.fromAction(() -> {
					memoryChunks.clear();
					log.debug("All memory chunks released -> Total memory allocated: 0 MB");
				})
				.doOnComplete(() -> ctx.response().setStatusCode(ACCEPTED.code()).end())
				.doOnError(err -> ctx.response().setStatusCode(INTERNAL_SERVER_ERROR.code()).end(err.getMessage()))
				.subscribe(CompletableHelper.nullObserver()));
	}

	private void registerGCRoute(Router router) {
		router.post("/clean")
			.handler(ctx -> Completable.fromAction(() -> {
					System.gc();
					log.debug("Garbage collection executed");
				})
				.doOnComplete(() -> ctx.response().setStatusCode(ACCEPTED.code()).end())
				.doOnError(err -> ctx.response().setStatusCode(INTERNAL_SERVER_ERROR.code()).end(err.getMessage()))
				.subscribe(CompletableHelper.nullObserver()));
	}

}
