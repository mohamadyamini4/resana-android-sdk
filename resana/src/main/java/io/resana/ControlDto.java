package io.resana;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

class ControlDto implements Parcelable, Serializable {

    @SerializedName("ch0")
    float nativeChance = 1;

    @SerializedName("ch1")
    float splashChance = 1;

    @SerializedName("ttl")
    int controlsTTL = 1000;

    @SerializedName("rl")
    String resanaLabel = ResanaInternal.DEFAULT_RESANA_INFO_TEXT;

    @SerializedName("bz")
    String[] blockedZones;

    @SerializedName("ch2")
    int videoStickyChance = 1;

    protected ControlDto(Parcel in) {
        nativeChance = in.readFloat();
        splashChance = in.readFloat();
        videoStickyChance = in.readInt();
        controlsTTL = in.readInt();
        resanaLabel = in.readString();
        blockedZones = in.createStringArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(nativeChance);
        dest.writeFloat(splashChance);
        dest.writeInt(videoStickyChance);
        dest.writeInt(controlsTTL);
        dest.writeString(resanaLabel);
        dest.writeStringArray(blockedZones);
    }

    public static final Creator<ControlDto> CREATOR = new Creator<ControlDto>() {
        @Override
        public ControlDto createFromParcel(Parcel in) {
            return new ControlDto(in);
        }

        @Override
        public ControlDto[] newArray(int size) {
            return new ControlDto[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}