package com.github.jorgecastillo.kotlinandroid.data.interpreter

import com.github.jorgecastillo.kotlinandroid.data.algebra.HeroesDataSource
import com.github.jorgecastillo.kotlinandroid.data.algebra.HeroesDataSourceAlgebra
import com.github.jorgecastillo.kotlinandroid.data.algebra.HeroesDataSourceAlgebraHK
import com.github.jorgecastillo.kotlinandroid.data.algebra.ev
import com.github.jorgecastillo.kotlinandroid.di.context.SuperHeroesContext
import com.github.jorgecastillo.kotlinandroid.di.context.SuperHeroesContext.GetHeroesContext
import com.github.jorgecastillo.kotlinandroid.domain.model.CharacterError
import com.github.jorgecastillo.kotlinandroid.domain.model.CharacterError.AuthenticationError
import com.github.jorgecastillo.kotlinandroid.domain.model.CharacterError.NotFoundError
import com.github.jorgecastillo.kotlinandroid.domain.model.CharacterError.UnknownServerError
import com.github.jorgecastillo.kotlinandroid.functional.AsyncResult
import com.github.jorgecastillo.kotlinandroid.functional.AsyncResultHK
import com.github.jorgecastillo.kotlinandroid.functional.AsyncResultKind
import com.github.jorgecastillo.kotlinandroid.functional.AsyncResultMonadReaderInstance
import com.github.jorgecastillo.kotlinandroid.functional.ev
import com.github.jorgecastillo.kotlinandroid.functional.monadReader
import com.karumi.marvelapiclient.MarvelApiException
import com.karumi.marvelapiclient.MarvelAuthApiException
import com.karumi.marvelapiclient.model.CharacterDto
import com.karumi.marvelapiclient.model.CharactersQuery.Builder
import kategory.FunctionK
import kategory.HK
import kategory.Option
import kategory.binding
import kategory.foldMap
import java.net.HttpURLConnection

fun test(): Unit {
  val heroesDS = object : HeroesDataSource {
  }

  val MR = AsyncResult.monadReader<GetHeroesContext>()
  heroesDS.getAll().foldMap(asyncResultDataSourceInterpreter(MR), MR)
}

inline fun <D : SuperHeroesContext> asyncResultDataSourceInterpreter(
    ARM: AsyncResultMonadReaderInstance<D>): FunctionK<HeroesDataSourceAlgebraHK, AsyncResultHK> =
    object : FunctionK<HeroesDataSourceAlgebraHK, AsyncResultHK> {
      override fun <A> invoke(fa: HK<HeroesDataSourceAlgebraHK, A>): AsyncResult<D, A> {
        val op = fa.ev()
        return when (op) {
          is HeroesDataSourceAlgebra.GetAll -> getAllHeroesAsyncResult(ARM) as HK<AsyncResultHK, A>
          is HeroesDataSourceAlgebra.GetSingle -> getHeroDetails(ARM, op.heroId) as HK<AsyncResultHK, A>
        }
      }
    }

fun <D : SuperHeroesContext> getAllHeroesAsyncResult(
    AR: AsyncResultMonadReaderInstance<D>): AsyncResult<D, List<CharacterDto>> {
  return AR.binding {
    val query = Builder.create().withOffset(0).withLimit(50).build()
    val ctx = AR.ask().bind()
    AR.catch(
        { ctx.apiClient.getAll(query).response.characters.toList() },
        { exceptionAsCharacterError(it) }
    )
  }.ev()
}

fun <D : SuperHeroesContext> getHeroDetails(AR: AsyncResultMonadReaderInstance<D>,
    heroId: String): AsyncResult<D, List<CharacterDto>> =
    AR.binding {
      val ctx = AR.ask().bind()
      AR.catch(
          { listOf(ctx.apiClient.getCharacter(heroId).response) },
          { exceptionAsCharacterError(it) }
      ).ev()
    }.ev()

fun exceptionAsCharacterError(e: Throwable): CharacterError =
    when (e) {
      is MarvelAuthApiException -> AuthenticationError
      is MarvelApiException ->
        if (e.httpCode == HttpURLConnection.HTTP_NOT_FOUND) NotFoundError
        else UnknownServerError(Option.Some(e))
      else -> UnknownServerError((Option.Some(e)))
    }
