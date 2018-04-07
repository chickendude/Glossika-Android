package ch.ralena.glossikaschedule.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import ch.ralena.glossikaschedule.R;
import ch.ralena.glossikaschedule.object.Language;
import io.reactivex.subjects.PublishSubject;
import io.realm.RealmResults;

public class LanguageListAdapter extends RecyclerView.Adapter<LanguageListAdapter.ViewHolder> {

	PublishSubject<Language> languageSubject = PublishSubject.create();

	public PublishSubject<Language> asObservable() {
		return languageSubject;
	}

	private Context context;
	private RealmResults<Language> languages;

	public LanguageListAdapter(Context context, RealmResults<Language> languages) {
		this.context = context;
		this.languages = languages;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view;
		view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_language, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (position < getItemCount())
			holder.bindView(languages.get(position));
	}

	@Override
	public int getItemCount() {
		return languages.size();
	}

	class ViewHolder extends RecyclerView.ViewHolder {
		private View view;
		private TextView languageName;
		private TextView scheduleType;
		private ImageView flagImage;
		private Language language;

		ViewHolder(View view) {
			super(view);
			this.view = view;
			languageName = view.findViewById(R.id.languageLabel);
			scheduleType = view.findViewById(R.id.scheduleTypeLabel);
			flagImage = view.findViewById(R.id.flagImageView);
			this.view.setOnClickListener(v -> languageSubject.onNext(language));
		}

		void bindView(Language language) {
			this.language = language;
			view.setBackgroundResource(R.drawable.menu_language);
			languageName.setText(language.getLongName());
//			scheduleType.setText(schedule.getTitle());
			flagImage.setImageResource(language.getLanguageType().getDrawable());
		}
	}
}
