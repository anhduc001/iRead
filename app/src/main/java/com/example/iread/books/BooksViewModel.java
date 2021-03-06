package com.example.iread.books;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Singleton
public class BooksViewModel extends ViewModel {
    private final BookRepository bookRepository;

    @Inject
    public BooksViewModel(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // Initialized
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private MutableLiveData<Boolean> mutableProgress = new MutableLiveData<>();
    public LiveData<Boolean> progress = mutableProgress;
    private MutableLiveData<List<Book>> mutableBooksListFull = new MutableLiveData<>();
    private MutableLiveData<List<Book>> mutableBooksList = new MutableLiveData<>();
    public LiveData<List<Book>> booksList = mutableBooksList;
    private MutableLiveData<String> mutableError = new MutableLiveData<>();
    public LiveData<String> error = mutableError;

    public void getBooksList() {
        Single<List<Book>> booksListObservable = bookRepository.getBooksList();
        compositeDisposable.add(booksListObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> {
                    mutableProgress.setValue(true);
                })
                .doFinally(() -> {
                    mutableProgress.setValue(false);
                })
                .subscribe(data -> {
                    mutableBooksListFull.setValue(data);
                    mutableBooksList.setValue(data);
                }, error -> {
                    mutableError.setValue("Unable to get book list");
                    throw(error);
                })
        );
    }
    public void manipulateBooksList(
            SortingMenuOptions sortingMenuOptions, GenreMenuOptions genreMenuOptions, RatingMenuOptions ratingMenuOptions) {
        mutableProgress.setValue(true);
        List<Book> booksListFull = mutableBooksListFull.getValue();
        if (genreMenuOptions != null) {
            booksListFull = booksListFull.stream()
                    .filter(book -> book.genre.name.equals(genreMenuOptions.getGenre()))
                    .collect(Collectors.toList());
        }
        if (ratingMenuOptions != null) {
            float ratingFrom = 0;
            float ratingTo = 5;
            switch (ratingMenuOptions) {
                case STAR_0_1:
                    ratingFrom = 0;
                    ratingTo = 1;
                    break;
                case STAR_1_2:
                    ratingFrom = 1;
                    ratingTo = 2;
                    break;
                case STAR_2_3:
                    ratingFrom = 2;
                    ratingTo = 3;
                    break;
                case STAR_3_4:
                    ratingFrom = 3;
                    ratingTo = 4;
                    break;
                case STAR_4_5:
                    ratingFrom = 4;
                    ratingTo = 5;
                    break;
            }
            final float finalRatingFrom = ratingFrom;
            final float finalRatingTo = ratingTo;

            booksListFull = booksListFull.stream()
                    .filter(book -> {
                        return (book.overallRating > finalRatingFrom) & (book.overallRating <= finalRatingTo);
                    })
                    .collect(Collectors.toList());
        }

        switch(sortingMenuOptions) {
            case NAME_ASCENDING:
                Collections.sort(booksListFull, new NameAscendingComparator());
                break;
            case NAME_DESCENDING:
                Collections.sort(booksListFull, new NameDescendingComparator());
                break;
            case TIME_ASCENDING:
                Collections.sort(booksListFull, new TimeAscendingComparator());
                break;
            case TIME_DESCENDING:
                Collections.sort(booksListFull, new TimeDescendingComparator());
                break;
        }
        mutableBooksList.setValue(booksListFull);
        mutableProgress.setValue(false);
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();
    }

}

class NameAscendingComparator implements Comparator<Book>
{
    @Override
    public int compare(Book o1, Book o2) {
        return o1.bookTitle.compareTo(o2.bookTitle);
    }
}

class NameDescendingComparator implements Comparator<Book>
{

    @Override
    public int compare(Book o1, Book o2) {
        return -o1.bookTitle.compareTo(o2.bookTitle);
    }
}

class TimeAscendingComparator implements Comparator<Book>
{

    @Override
    public int compare(Book o1, Book o2) {
        try {
            return Integer.parseInt(o1.published) - Integer.parseInt(o2.published);
        } catch (Exception e) {
            return 0;
        }
    }
}

class TimeDescendingComparator implements Comparator<Book>
{

    @Override
    public int compare(Book o1, Book o2) {
        try {
            return -(Integer.parseInt(o1.published) - Integer.parseInt(o2.published));
        } catch (Exception e) {
            return 0;
        }
    }
}
