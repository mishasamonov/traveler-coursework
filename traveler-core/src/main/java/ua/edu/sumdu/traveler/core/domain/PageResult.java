package ua.edu.sumdu.traveler.core.domain;

import java.util.List;

/** One page of a list, independent of Spring Data. page starts from 0. */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

  public int totalPages() {
    return size == 0 ? 0 : (int) ((totalElements + size - 1) / size);
  }

  public boolean hasPrevious() {
    return page > 0;
  }

  public boolean hasNext() {
    return page + 1 < totalPages();
  }

  public boolean isEmpty() {
    return content.isEmpty();
  }
}
