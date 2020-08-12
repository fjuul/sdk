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

        /**
         * @param birthDate required parameter for the user creation.
         */
        public void setBirthDate(@Nullable LocalDate birthDate) {
            this.birthDate = birthDate;
        }

        /**
         * @param gender required parameter for the user creation.
         */
        public void setGender(@Nullable Gender gender) {
            this.gender = gender;
        }

        /**
         * @param height required parameter for the user creation.
         */
        public void setHeight(float height) {
            this.height = height;
        }

        /**
         * @param weight required parameter for the user creation.
         */
        public void setWeight(float weight) {
            this.weight = weight;
        }

        /**
         * @param timezone local timezone of a user. If this is omitted, then default timezone will be used.
         */
        public void setTimezone(@Nullable TimeZone timezone) {
            this.timezone = timezone;
        }

        @Nullable
        public TimeZone getTimezone() {
            return timezone;
        }

        /**
         * @param locale language tag (code, e.g. 'fr', 'de', 'ru') of preferable locale for a user.
         *               If this is omitted, then first preferable locale will be taken.
         */
        public void setLocale(@Nullable String locale) {
            this.locale = locale;
        }

        @Nullable
        public String getLocale() {
            return locale;
        }
    }
}
