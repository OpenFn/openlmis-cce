/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.cce.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openlmis.cce.dto.BaseDto;
import org.openlmis.cce.util.DynamicPageTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;

public abstract class ResourceCommunicationService<T extends BaseDto>
    extends BaseCommunicationService {

  protected abstract Class<T> getResultClass();

  protected abstract Class<T[]> getArrayResultClass();

  /**
   * Return one object from service.
   *
   * @param id UUID of requesting object.
   * @return Requesting reference data object.
   */
  public T findById(UUID id) {
    try {
      return execute(id.toString(), null, null, null, HttpMethod.GET, getResultClass()).getBody();
    } catch (HttpStatusCodeException ex) {
      // rest template will handle 404 as an exception, instead of returning null
      if (HttpStatus.NOT_FOUND == ex.getStatusCode()) {
        logger.warn(
            "{} matching params does not exist.",
            getResultClass().getSimpleName()
        );

        return null;
      }

      throw DataRetrievalException.build(getResultClass().getSimpleName(), ex);
    }
  }

  public List<T> findAll() {
    return findAll("", null, getArrayResultClass());
  }

  /**
   * Return all reference data T objects.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @return all reference data T objects.
   */
  protected List<T> findAll(String resourceUrl, RequestParameters parameters) {
    return findAll(resourceUrl, parameters, getArrayResultClass());
  }

  protected <P> List<P> findAll(String resourceUrl, RequestParameters parameters, Class<P[]> type) {
    try {
      return Stream
          .of(execute(resourceUrl, parameters, null, null, HttpMethod.GET, type).getBody())
          .collect(Collectors.toList());
    } catch (HttpStatusCodeException ex) {
      throw DataRetrievalException.build(getResultClass().getSimpleName(), ex);
    }
  }

  protected <P> ServiceResponse<List<P>> tryFindAll(String resourceUrl, Class<P[]> type,
                                                    String etag) {
    try {
      RequestHeaders headers = RequestHeaders.init().setIfNoneMatch(etag);
      ResponseEntity<P[]> response = execute(
          resourceUrl, null, headers, null, HttpMethod.GET, type
      );

      if (response.getStatusCode() == HttpStatus.NOT_MODIFIED) {
        return new ServiceResponse<>(null, response.getHeaders(), false);
      } else {
        List<P> list = Stream.of(response.getBody()).collect(Collectors.toList());
        return new ServiceResponse<>(list, response.getHeaders(), true);
      }
    } catch (HttpStatusCodeException ex) {
      throw DataRetrievalException.build(getResultClass().getSimpleName(), ex);
    }
  }

  /**
   * Return all reference data T objects for Page that need to be retrieved with GET request.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @return Page of reference data T objects.
   */
  protected Page<T> getPage(String resourceUrl, RequestParameters parameters) {
    return getPage(resourceUrl, parameters, null, HttpMethod.GET);
  }

  /**
   * Return all reference data T objects for Page that need to be retrieved with POST request.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @param payload     body to include with the outgoing request.
   * @return Page of reference data T objects.
   */
  protected Page<T> getPage(String resourceUrl, RequestParameters parameters, Object payload) {
    return getPage(resourceUrl, parameters, payload, HttpMethod.POST);
  }

  /**
   * Return all reference data T objects for Page that need to be retrieved with given request.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @param payload     body to include with the outgoing request.
   * @param method      method for the request.
   * @return Page of reference data T objects.
   */
  protected Page<T> getPage(String resourceUrl, RequestParameters parameters, Object payload,
      HttpMethod method) {
    try {
      DynamicPageTypeReference<T> type = new DynamicPageTypeReference<>(getResultClass());
      return execute(resourceUrl, parameters, null, payload, method, type).getBody();
    } catch (HttpStatusCodeException ex) {
      throw DataRetrievalException.build(getResultClass().getSimpleName(), ex);
    }
  }
}
