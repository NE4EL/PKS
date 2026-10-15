package ru.mirea.studenthub.android.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.widget.EditText;

/** Подключает к EditText маску +7-999-999-99-99 и запрещает буквенный ввод. */
public final class PhoneMask {
    private PhoneMask() {
    }

    public static void attach(EditText input) {
        // Клавиатура принимает только цифры; + и дефисы вставляет исключительно форматтер.
        input.setKeyListener(DigitsKeyListener.getInstance("0123456789"));

        input.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus && input.getText().length() == 0) {
                input.setText("+7-");
                input.setSelection(input.length());
            } else if (!hasFocus && "+7-".contentEquals(input.getText())) {
                input.setText("");
            }
        });

        input.addTextChangedListener(new TextWatcher() {
            private boolean internalChange;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable editable) {
                if (internalChange) return;

                String formatted = PhoneFormatter.format(editable.toString());
                if (formatted.equals(editable.toString())) return;

                internalChange = true;
                input.setText(formatted);
                input.setSelection(input.length());
                internalChange = false;
            }
        });
    }
}
