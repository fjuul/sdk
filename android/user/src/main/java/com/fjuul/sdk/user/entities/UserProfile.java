package com.fjuul.sdk.user.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.TimeZone;

public class UserProfile {
    @NonNull private String token;
    @NonNull private LocalDate birthDate;
    @NonNull private Gender gender;
    @NonNull private float height;
    @NonNull private float weight;
    @NonNull private TimeZone timezone;

    private UserProfile(@NonNull LocalDate birthDate, @NonNull Gender gender, float height,
                        float weight, @NonNull TimeZone timezone, @NonNull String locale) {
        this.token = token;
        this.birthDate = birthDate;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.timezone = timezone;
        this.locale = locale;
    }

    @NonNull private String locale;

    @NonNull
    public String getToken() {
        return token;
    }

    @NonNull
    public LocalDate getBirthDate() {
        return birthDate;
    }

    @NonNull
    public Gender getGender() {
        return gender;
    }

    public float getHeight() {
        return height;
    }

    public float getWeight() {
        return weight;
    }

    @NonNull
    public TimeZone getTimezone() {
        return timezone;
    }

    @NonNull
    public String getLocale() {
        return locale;
    }

    public static final class PartialBuilder {
        // TODO: add predicate to validate fields ?
        @Nullable private LocalDate birthDate;
        @Nullable private Gender gender;
        @Nullable private Float height;
        @Nullable private Float weight;
        @Nullable private TimeZone timezone;
        @Nullable private String locale;

        public void setBirthDate(@Nullable LocalDate birthDate) {
            this.birthDate = birthDate;
        }

        public void setGender(@Nullable Gender gender) {
            this.gender = gender;
        }

        public void setHeight(float height) {
            this.height = height;
        }

        public void setWeight(float weight) {
            this.weight = weight;
        }

        public void setTimezone(@Nullable TimeZone timezone) {
            this.timezone = timezone;
        }

        @Nullable
        public TimeZone getTimezone() {
            return timezone;
        }

        public void setLocale(@Nullable String locale) {
            this.locale = locale;
        }

        @Nullable
        public String getLocale() {
            return locale;
        }
    }
}
