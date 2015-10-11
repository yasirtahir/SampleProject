package com.sampleproject.api;

import com.sampleproject.models.Places;

import java.util.ArrayList;

import retrofit.http.GET;
import retrofit.http.Path;
import rx.Observable;

public interface GoEuro {
    // http://api.goeuro.com/api/v2/position/suggest/{locale}/{term}
    @GET("/position/suggest/{locale}/{term}")
    public Observable<ArrayList<Places>> suggest(
            @Path("locale") String locale,
            @Path("term") String term
    );


}