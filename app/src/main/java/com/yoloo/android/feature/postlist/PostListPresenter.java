package com.yoloo.android.feature.postlist;

import com.yoloo.android.data.Response;
import com.yoloo.android.data.model.AccountRealm;
import com.yoloo.android.data.model.PostRealm;
import com.yoloo.android.data.repository.post.PostRepository;
import com.yoloo.android.data.repository.user.UserRepository;
import com.yoloo.android.data.sorter.PostSorter;
import com.yoloo.android.framework.MvpPresenter;
import com.yoloo.android.util.Pair;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.util.List;
import javax.annotation.Nonnull;

class PostListPresenter extends MvpPresenter<PostListView> {

  private final PostRepository postRepository;
  private final UserRepository userRepository;

  private String cursor;

  PostListPresenter(PostRepository postRepository, UserRepository userRepository) {
    this.postRepository = postRepository;
    this.userRepository = userRepository;
  }

  void loadPostsByGroup(boolean pullToRefresh, @Nonnull String groupId, @Nonnull PostSorter sorter,
      int limit) {
    getView().onLoading(pullToRefresh);

    shouldResetCursor(pullToRefresh);

    Observable<Response<List<PostRealm>>> postsObservable = postRepository
        .listByGroup(groupId, sorter, cursor, limit)
        .observeOn(AndroidSchedulers.mainThread(), true);

    Disposable d = Observable
        .zip(getUserObservable(), postsObservable, Pair::create)
        .subscribe(this::showData, this::showError);

    getDisposable().add(d);
  }

  void loadPostsByTag(boolean pullToRefresh, @Nonnull String tagName, @Nonnull PostSorter sorter,
      int limit) {
    getView().onLoading(pullToRefresh);

    shouldResetCursor(pullToRefresh);

    Observable<Response<List<PostRealm>>> postsObservable = postRepository
        .listByTags(tagName, sorter, cursor, limit)
        .observeOn(AndroidSchedulers.mainThread(), true);

    Disposable d = Observable
        .zip(getUserObservable(), postsObservable, Pair::create)
        .subscribe(this::showData, this::showError);

    getDisposable().add(d);
  }

  void loadPostsByUser(boolean pullToRefresh, @Nonnull String userId, int limit) {
    getView().onLoading(pullToRefresh);

    shouldResetCursor(pullToRefresh);

    Observable<Response<List<PostRealm>>> postsObservable = postRepository
        .listByUser(userId, cursor, limit)
        .observeOn(AndroidSchedulers.mainThread(), true);

    Disposable d = Observable
        .zip(getUserObservable(), postsObservable, Pair::create)
        .subscribe(this::showData, this::showError);

    getDisposable().add(d);
  }

  void loadPostsByBounty(boolean pullToRefresh, int limit) {
    getView().onLoading(pullToRefresh);

    shouldResetCursor(pullToRefresh);

    Observable<Response<List<PostRealm>>> postsObservable =
        postRepository.listByBounty(cursor, limit).observeOn(AndroidSchedulers.mainThread(), true);

    Disposable d = Observable
        .zip(getUserObservable(), postsObservable, Pair::create)
        .subscribe(this::showData, this::showError);

    getDisposable().add(d);
  }

  void loadPostsByBookmarked(boolean pullToRefresh, int limit) {
    getView().onLoading(pullToRefresh);

    shouldResetCursor(pullToRefresh);

    Observable<Response<List<PostRealm>>> postsObservable = postRepository
        .listByBookmarked(cursor, limit)
        .observeOn(AndroidSchedulers.mainThread(), true);

    Disposable d = Observable
        .zip(getUserObservable(), postsObservable, Pair::create)
        .subscribe(this::showData, this::showError);

    getDisposable().add(d);
  }

  void deletePost(@Nonnull String postId) {
    Disposable d =
        postRepository.deletePost(postId).observeOn(AndroidSchedulers.mainThread()).subscribe();

    getDisposable().add(d);
  }

  void votePost(@Nonnull String postId, int direction) {
    Disposable d = postRepository
        .votePost(postId, direction)
        .andThen(postRepository.getPost(postId))
        .observeOn(AndroidSchedulers.mainThread(), true)
        .subscribe(post -> {
          if (post.isPresent()) {
            getView().onPostUpdated(post.get());
          }
        }, this::showError);

    getDisposable().add(d);
  }

  void bookmarkPost(@Nonnull String postId) {
    Disposable d =
        postRepository.bookmarkPost(postId).observeOn(AndroidSchedulers.mainThread()).subscribe();

    getDisposable().add(d);
  }

  void unBookmarkPost(@Nonnull String postId) {
    Disposable d =
        postRepository.unBookmarkPost(postId).observeOn(AndroidSchedulers.mainThread()).subscribe();

    getDisposable().add(d);
  }

  private void showData(Pair<AccountRealm, Response<List<PostRealm>>> pair) {
    if (pair.second.getData().isEmpty()) {
      getView().onEmpty();
    } else {
      getView().onAccountLoaded(pair.first);

      cursor = pair.second.getCursor();
      getView().onLoaded(pair.second.getData());
      getView().showContent();
    }
  }

  private void showError(Throwable throwable) {
    getView().onError(throwable);
  }

  private Observable<AccountRealm> getUserObservable() {
    return userRepository.getLocalMe().toObservable().observeOn(AndroidSchedulers.mainThread());
  }

  private void shouldResetCursor(boolean pullToRefresh) {
    if (pullToRefresh) {
      cursor = null;
    }
  }
}
