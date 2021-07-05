package ch.ralena.natibo.ui.adapter;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ch.ralena.natibo.R;
import ch.ralena.natibo.data.LanguageType;
import io.reactivex.subjects.PublishSubject;

public class LanguageSelectAdapter extends RecyclerView.Adapter<LanguageSelectAdapter.LanguageViewHolder> {
	List<LanguageType> languages;
	LanguageType selectedLanguage;

	PublishSubject<LanguageType> observable = PublishSubject.create();


	public PublishSubject<LanguageType> asObservable() {
		return observable;
	}

	public LanguageSelectAdapter(List<LanguageType> languages, LanguageType selectedLanguage) {
		this.languages = languages;
		this.selectedLanguage = selectedLanguage;
	}

	@Override
	public LanguageSelectAdapter.LanguageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language, parent, false);
		return new LanguageViewHolder(view);
	}

	@Override
	public void onBindViewHolder(LanguageSelectAdapter.LanguageViewHolder holder, int position) {
		holder.bindView(languages.get(position));
	}

	@Override
	public int getItemCount() {
		return languages.size();
	}

	class LanguageViewHolder extends RecyclerView.ViewHolder {
		TextView languageName;
		ImageView flag;

		LanguageViewHolder(View itemView) {
			super(itemView);
			flag = itemView.findViewById(R.id.flag_image);
			languageName = itemView.findViewById(R.id.languageLabel);
		}

		private void bindView(LanguageType language) {
			if (language.equals(selectedLanguage)) {
				itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorAccent));
			} else {
				itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), android.R.color.transparent));
			}
			itemView.setOnClickListener(view -> {
				selectedLanguage = language;
				notifyDataSetChanged();
				observable.onNext(language);
			});
			flag.setImageResource(language.getDrawable());
			languageName.setText(language.getName());
		}
	}
}
