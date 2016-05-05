package io.vertx.blueprint.todolist.service;

import io.vertx.blueprint.todolist.Constants;
import io.vertx.blueprint.todolist.Utils;
import io.vertx.blueprint.todolist.entity.Todo;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Vert.x Blueprint Application - Todo Backend
 * Todo Service Redis Implementation
 *
 * @author Eric Zhao
 */
public class RedisTodoService implements TodoService {

  private final Vertx vertx;
  private final RedisOptions config;
  private final RedisClient redis;

  public RedisTodoService() {
    this(Vertx.vertx());
  }

  public RedisTodoService(RedisOptions config) {
    this(Vertx.vertx(), config);
  }

  public RedisTodoService(Vertx vertx) {
    this(vertx, new RedisOptions());
  }

  public RedisTodoService(Vertx vertx, RedisOptions config) {
    this.vertx = vertx;
    this.config = config;
    this.redis = RedisClient.create(vertx, config);
  }

  @Override
  public Future<Boolean> initData() {
    return insert(new Todo(Math.abs(new java.util.Random().nextInt()),
      "Something to do...", false, 1, "todo/ex"));
  }

  @Override
  public Future<Boolean> insert(Todo todo) {
    Future<Boolean> result = Future.future();
    final String encoded = Json.encodePrettily(todo);
    redis.hset(Constants.REDIS_TODO_KEY, String.valueOf(todo.getId()),
      encoded, res -> {
        if (res.succeeded())
          result.complete(true);
        else
          result.fail(res.cause());
      });
    return result;
  }

  @Override
  public Future<List<Todo>> getAll() {
    Future<List<Todo>> result = Future.future();
    redis.hvals(Constants.REDIS_TODO_KEY, res -> {
      if (res.succeeded()) {
        result.complete(res.result()
          .stream()
          .map(x -> Utils.getTodoFromJson((String) x))
          .collect(Collectors.toList()));
      } else
        result.fail(res.cause());
    });
    return result;
  }

  @Override
  public Future<Todo> getCertain(String todoID) {
    Future<Todo> result = Future.future();
    redis.hget(Constants.REDIS_TODO_KEY, todoID, res -> {
      if (res.succeeded()) {
        result.complete(Utils.getTodoFromJson(res.result()));
      } else
        result.fail(res.cause());
    });
    return result;
  }

  @Override
  public Future<Todo> update(String todoId, Todo newTodo) {
    Future<Todo> result = Future.future();
    redis.hget(Constants.REDIS_TODO_KEY, todoId, x -> {
      if (x.succeeded()) {
        String res = x.result();
        if (res == null)
          result.complete(null);
        else {
          Todo oldTodo = Utils.getTodoFromJson(res);
          Todo fnTodo = oldTodo.merge(newTodo);
          String fnTodoStr = Json.encodePrettily(fnTodo);
          redis.hset(Constants.REDIS_TODO_KEY, todoId, fnTodoStr, r -> {
            if (r.succeeded()) {
              result.complete(fnTodo);
            } else {
              result.fail(r.cause());
            }
          });
        }
      } else {
        result.fail(x.cause());
      }
    });
    return result;
  }

  @Override
  public Future<Boolean> delete(String todoId) {
    Future<Boolean> result = Future.future();
    redis.hdel(Constants.REDIS_TODO_KEY, todoId, res -> {
      if (res.succeeded())
        result.complete(true);
      else
        result.complete(false);
    });
    return result;
  }

  @Override
  public Future<Boolean> deleteAll() {
    Future<Boolean> result = Future.future();
    redis.del(Constants.REDIS_TODO_KEY, res -> {
      if (res.succeeded())
        result.complete(true);
      else
        result.complete(false);
    });
    return result;
  }
}
