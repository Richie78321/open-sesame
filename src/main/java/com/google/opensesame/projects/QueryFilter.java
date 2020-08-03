package com.google.opensesame.projects;

import com.google.opensesame.util.ServletValidationException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;

/**
 * A Datastore filter stored in the form used by Objectify when loading entities from the Datastore.
 *
 * <p>See here for how Objectify conducts filtering on a query:
 * https://www.javadoc.io/static/com.googlecode.objectify/objectify/6.0.6/com/googlecode/objectify/cmd/Query.html#filter(java.lang.String,java.lang.Object)
 */
class QueryFilter {
  public static final String FILTER_QUERY_REGEX = "^[A-Za-z]+ (>|>=|!=|=|<|<=) [^\s]+$";

  /**
   * Parses a QueryFilter from a string and responds with errors to the provided servlet response
   * object.
   *
   * <p>QueryFilter strings should be in the format "fieldName <comparator> value". Examples:
   *
   * <blockquote>
   *
   * <pre>
   * "numMentors >= 2"
   * "numInterestedUsers = 0"
   * </pre>
   *
   * </blockquote>
   *
   * @param QueryFilterString The filter query string.
   * @param response The servlet response to send errors to.
   * @return Returns the QueryFilter or null if there was an error parsing the string.
   * @throws IOException
   */
  public static QueryFilter fromString(String QueryFilterString)
      throws IOException, ServletValidationException {
    if (!QueryFilterString.matches(FILTER_QUERY_REGEX)) {
      throw new ServletValidationException(
          "Invalid filter query.",
          "Unable to query for projects.",
          HttpServletResponse.SC_BAD_REQUEST);
    }

    String[] splitFilterRequest = QueryFilterString.split(" ");
    String filterFieldName = splitFilterRequest[0];
    Optional<Field> filterField =
        ProjectQuery.queryableFields.stream()
            .filter((field) -> field.getName().equals(filterFieldName))
            .findFirst();
    if (!filterField.isPresent()) {
      throw new ServletValidationException(
          "Cannot filter by the field '" + filterFieldName + "'.",
          "Unable to query for projects.",
          HttpServletResponse.SC_BAD_REQUEST);
    }

    String comparisonValue = splitFilterRequest[2];
    Object comparisonObject;
    try {
      comparisonObject =
          filterField.get().getType().getConstructor(String.class).newInstance(comparisonValue);
    } catch (Exception e) {
      // TODO(Richie): Handle the exceptions more specifically.
      throw new ServletValidationException(
          "Cannot parse the comparison value '"
              + comparisonValue
              + "' for the field '"
              + filterFieldName
              + "'.",
          "Unable to query for projects.",
          HttpServletResponse.SC_BAD_REQUEST);
    }

    String comparator = splitFilterRequest[1];
    return new QueryFilter(filterFieldName + " " + comparator, comparisonObject);
  }

  public final String condition;
  public final Object comparisonObject;

  public QueryFilter(String condition, Object comparisonObject) {
    this.condition = condition;
    this.comparisonObject = comparisonObject;
  }
}