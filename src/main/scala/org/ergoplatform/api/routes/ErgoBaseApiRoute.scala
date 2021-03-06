package org.ergoplatform.api.routes

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.{Directive, Directive1, Route}
import akka.http.scaladsl.server.Directives._
import io.circe.Json
import org.ergoplatform.local.ErgoStatsCollector.NodeInfo
import org.ergoplatform.modifiers.history.Header
import scorex.core.ModifierId
import scorex.core.api.http.ApiRoute
import scorex.core.serialization.SerializerRegistry
import scorex.core.serialization.SerializerRegistry.SerializerRecord
import scorex.crypto.encode.Base58

import scala.concurrent.Future
import scala.util.Success

trait ErgoBaseApiRoute extends ApiRoute {

  import org.ergoplatform.utils.JsonSerialization.serializerReg

  implicit val ec = context.dispatcher

  protected def toJsonResponse(js: Json): Route = {
    val resp = complete(HttpEntity(ContentTypes.`application/json`, js.spaces2))
    withCors(resp)
  }

  protected def toJsonResponse(fn: Future[Json]): Route = onSuccess(fn) { toJsonResponse }

  protected def toJsonOptionalResponse(fn: Future[Option[Json]]): Route = {
    onSuccess(fn) {
      case Some(v) => toJsonResponse(v)
      case None => withCors(complete(StatusCodes.NotFound))
    }
  }

  val paging: Directive[(Int, Int)] = parameters("offset".as[Int] ? 0, "limit".as[Int] ? 50)

  val headerId: Directive1[ModifierId] = pathPrefix(Segment).flatMap { h =>
    Base58.decode(h) match {
      case Success(header) => provide(ModifierId @@ header)
      case _ => reject
    }
  }

  implicit class OkJsonResp(fn: Future[Json]) {
    def okJson(): Route = toJsonResponse(fn)
  }

  implicit class OkJsonOptResp(fn: Future[Option[Json]]) {
    def okJson(): Route = toJsonOptionalResponse(fn)
  }
}
