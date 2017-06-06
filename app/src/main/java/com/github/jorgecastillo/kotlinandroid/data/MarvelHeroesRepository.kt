package com.github.jorgecastillo.kotlinandroid.data

import com.github.jorgecastillo.kotlinandroid.di.context.GetHeroesContext
import katz.Id
import katz.Reader

class MarvelHeroesRepository {

  fun getHeroes() = Reader.ask<GetHeroesContext>(Id).flatMap {
    it.networkDataSource.getAll()
  }

  fun getHeroesFromAvengerComics() = Reader.ask<GetHeroesContext>(Id).flatMap {
    it.networkDataSource.getHeroesFromAvengerComics()
  }
}