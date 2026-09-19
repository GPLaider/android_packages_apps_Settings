package com.android.settings.security;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.PasswordValidationError;
import com.android.settings.R;
import com.android.settings.password.ChooseLockPassword;
import com.android.settings.password.ChooseLockPassword.ChooseLockPasswordFragment
        .PasswordValidationErrorConverter;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class DuressPasswordSetupActivity extends DuressPasswordActivity
        implements TextView.OnEditorActionListener {
    static final String TAG = DuressPasswordSetupActivity.class.getSimpleName();
    static final String EXTRA_USER_CREDENTIAL = "user_credential";
    static final String EXTRA_TITLE_TEXT = "title";

    private boolean isUpdate;

    enum DuressCredentialType {
        PIN(LockscreenCredential::createPin,
                R.id.pin_input, R.id.pin_input_confirmation,
                R.string.lockpassword_confirm_pins_dont_match),
        PASSWORD(LockscreenCredential::createPassword,
                R.id.password_input, R.id.password_input_confirmation,
                R.string.lockpassword_confirm_passwords_dont_match);

        final Function<String, LockscreenCredential> credentialCreator;
        final @IdRes int mainInputId;
        final @IdRes int confirmationInputId;
        final @StringRes int confirmationMismatchText;

        DuressCredentialType(Function<String, LockscreenCredential> credentialCreator,
                int mainInputId, int confirmationInputId,
                int confirmationMismatchText) {
            this.credentialCreator = credentialCreator;
            this.mainInputId = mainInputId;
            this.confirmationInputId = confirmationInputId;
            this.confirmationMismatchText = confirmationMismatchText;
        }

        LockscreenCredential createCredential(DuressPasswordSetupActivity activity) {
            EditText input = activity.requireViewById(mainInputId);
            return credentialCreator.apply(input.getText().toString());
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.duress_password_setup);

        GlifLayout layout = requireViewById(R.id.glif_layout);
        int headerText = getIntent().getExtras().getInt(EXTRA_TITLE_TEXT);
        isUpdate = headerText == R.string.duress_pwd_action_update;
        layout.setHeaderText(headerText);
        adjustDescriptionStyle(layout);

        FooterBarMixin footerBar = layout.getMixin(FooterBarMixin.class);
        var primaryBuilder = new FooterButton.Builder(this);
        primaryBuilder.setText(
                isUpdate ? R.string.duress_pwd_update_button : R.string.duress_pwd_add_button);
        primaryBuilder.setButtonType(FooterButton.ButtonType.DONE);
        primaryBuilder.setListener(view -> save());
        primaryBuilder.setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary);
        footerBar.setPrimaryButton(primaryBuilder.build());

        var secondaryBuilder = new FooterButton.Builder(this);
        secondaryBuilder.setText(R.string.duress_pwd_cancel_button);
        secondaryBuilder.setButtonType(FooterButton.ButtonType.CANCEL);
        secondaryBuilder.setListener(view -> finish());
        secondaryBuilder.setTheme(
                com.google.android.setupdesign.R.style.SudGlifButton_Secondary);
        footerBar.setSecondaryButton(secondaryBuilder.build());

        for (var credentialType : DuressCredentialType.values()) {
            for (int id : new int[] {
                    credentialType.mainInputId, credentialType.confirmationInputId}) {
                EditText input = requireViewById(id);
                input.setTag(credentialType);
                input.setOnEditorActionListener(this);
            }
        }
    }

    @Override
    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (actionId != EditorInfo.IME_ACTION_NEXT
                && actionId != EditorInfo.IME_ACTION_DONE) {
            return false;
        }

        EditText input = (EditText) view;
        DuressCredentialType credentialType =
                (DuressCredentialType) input.getTag();
        if (view.getId() == credentialType.mainInputId) {
            return !checkCredentialInputErrors(credentialType);
        }
        if (view.getId() == credentialType.confirmationInputId) {
            return !checkCredentialConfirmationError(credentialType);
        }
        return false;
    }

    private void save() {
        boolean credentialsValid = true;
        for (DuressCredentialType credentialType : DuressCredentialType.values()) {
            credentialsValid &= checkCredentialInputErrors(credentialType);
            credentialsValid &= checkCredentialConfirmationError(credentialType);
        }
        if (!credentialsValid) {
            return;
        }

        var userCredential = getIntent().getParcelableExtra(
                EXTRA_USER_CREDENTIAL, LockscreenCredential.class);
        Objects.requireNonNull(userCredential, EXTRA_USER_CREDENTIAL);
        LockPatternUtils lockPatternUtils = getLockPatternUtils();

        Runnable saveCredentials = () -> {
            LockscreenCredential pin = DuressCredentialType.PIN.createCredential(this);
            LockscreenCredential password =
                    DuressCredentialType.PASSWORD.createCredential(this);
            try {
                lockPatternUtils.setDuressCredentials(userCredential, pin, password);
            } catch (Exception e) {
                Log.e(TAG, "setDuressCredentials failed", e);
                var dialog = new AlertDialog.Builder(this);
                dialog.setMessage(getString(R.string.duress_pwd_save_error, e.toString()));
                dialog.setNeutralButton(R.string.duress_pwd_error_dialog_dismiss, null);
                dialog.show();
                return;
            } finally {
                pin.zeroize();
                password.zeroize();
            }
            Toast.makeText(this,
                    isUpdate
                            ? R.string.duress_pwd_toast_updated
                            : R.string.duress_pwd_toast_added,
                    Toast.LENGTH_LONG).show();
            finish();
        };

        if (isUpdate) {
            saveCredentials.run();
        } else {
            var warning = new AlertDialog.Builder(this);
            warning.setTitle(R.string.duress_pwd_save_warning_title);
            warning.setMessage(R.string.duress_pwd_save_warning_text);
            warning.setNegativeButton(R.string.duress_pwd_cancel_button, null);
            warning.setPositiveButton(
                    R.string.duress_pwd_proceed_button,
                    (dialog, which) -> saveCredentials.run());
            warning.show();
        }
    }

    private boolean checkCredentialInputErrors(DuressCredentialType credentialType) {
        EditText input = requireViewById(credentialType.mainInputId);
        LockscreenCredential credential =
                credentialType.credentialCreator.apply(input.getText().toString());

        List<PasswordValidationError> errors =
                LockPatternUtils.validateDuressCredential(credential);
        String error = null;
        boolean valid = true;
        if (!errors.isEmpty()) {
            valid = false;
            var converter = new PasswordValidationErrorConverter(
                    this,
                    credential.isPassword(),
                    ChooseLockPassword.ChooseLockPasswordFragment.ProfileType.None,
                    errors);
            error = String.join("\n", converter.convertErrorCodeToMessages());
        }
        input.setError(error);
        credential.zeroize();
        return valid;
    }

    private boolean checkCredentialConfirmationError(
            DuressCredentialType credentialType) {
        EditText mainInput = requireViewById(credentialType.mainInputId);
        EditText confirmationInput =
                requireViewById(credentialType.confirmationInputId);
        boolean matches = mainInput.getText().toString().equals(
                confirmationInput.getText().toString());
        confirmationInput.setError(
                matches ? null : getText(credentialType.confirmationMismatchText));
        return matches;
    }
}
