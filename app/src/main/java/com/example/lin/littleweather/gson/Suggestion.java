package com.example.lin.littleweather.gson;

import com.google.gson.annotations.SerializedName;

public class Suggestion {

    @SerializedName("comf")
    public Confort comfort;

    @SerializedName("cw")
    public CarWash carwash;

    public Sport sport;

    public class Confort {
        @SerializedName("txt")
       public String info;
    }

    public class CarWash {
        @SerializedName("txt")
        public String info;
    }

    public class Sport {
        @SerializedName("txt")
        public String info;
    }
}
