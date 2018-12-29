package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

class NativeDto extends AdDto implements Parcelable, Serializable {

    @SerializedName("texts")
    TextDto texts;


    protected NativeDto(Parcel in) {
        super(in);
        texts = in.readParcelable(TextDto.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(texts, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<NativeDto> CREATOR = new Creator<NativeDto>() {
        @Override
        public NativeDto createFromParcel(Parcel in) {
            return new NativeDto(in);
        }

        @Override
        public NativeDto[] newArray(int size) {
            return new NativeDto[size];
        }
    };
}
