package dev.notune.transcribe;

import android.content.Context;
import android.view.View;

import com.google.android.material.button.MaterialButtonToggleGroup;

/**
 * The English / international switch of voice input (layout
 * {@code view_model_choice}). Only shown while an English model is selected
 * in the Models screen. The choice is remembered for the next recording and
 * reported to the listener, which hands it to the native session: switching
 * is allowed until the recording stops, and the model selected at that
 * moment transcribes.
 */
final class ModelChoiceToggle {
    interface Listener {
        void onModelChoice(boolean english);
    }

    private final Context context;
    private final MaterialButtonToggleGroup group;
    /** Set while the checked button is changed in code, to not report it. */
    private boolean updating = false;

    private ModelChoiceToggle(Context context, MaterialButtonToggleGroup group, Listener listener) {
        this.context = context;
        this.group = group;
        group.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked || updating) return;
            boolean english = id == R.id.model_choice_english;
            ModelStore.setUseEnglish(context, english);
            listener.onModelChoice(english);
        });
    }

    /** Binds the switch inside {@code root}; null if the layout has none. */
    static ModelChoiceToggle bind(Context context, View root, Listener listener) {
        MaterialButtonToggleGroup group = root.findViewById(R.id.model_choice);
        if (group == null) return null;
        ModelChoiceToggle toggle = new ModelChoiceToggle(context, group, listener);
        toggle.refresh();
        return toggle;
    }

    /** Re-reads whether the switch is offered and which model is selected. */
    void refresh() {
        boolean offered = ModelStore.hasEnglishModel(context);
        group.setVisibility(offered ? View.VISIBLE : View.GONE);
        updating = true;
        group.check(offered && ModelStore.useEnglish(context)
                ? R.id.model_choice_english : R.id.model_choice_international);
        updating = false;
    }

    /** Enabled until the recording stops; disabled while transcribing. */
    void setEnabled(boolean enabled) {
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(enabled);
        }
        group.setEnabled(enabled);
    }
}
