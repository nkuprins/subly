package com.app.subly.dto.form;

import com.app.subly.model.provider.SubscriptionProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class CreateProviderForm {

    @NotBlank(message = "Please enter a valid provider name")
    @Size(
            max = SubscriptionProvider.NAME_MAX_LENGTH,
            message = "The name must not larger than " + SubscriptionProvider.NAME_MAX_LENGTH + " symbols.")
    String name;

    @NotNull(message = "Please choose a valid category")
    @Positive(message = "The id must be positive")
    Long categoryId;

    @Size(
            max = SubscriptionProvider.LOGO_PATH_MAX_LENGTH,
            message = "The logo path must not larger than " + SubscriptionProvider.LOGO_PATH_MAX_LENGTH + " symbols.")
    String logoPath;
}
