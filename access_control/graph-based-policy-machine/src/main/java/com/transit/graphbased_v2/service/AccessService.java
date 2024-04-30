package com.transit.graphbased_v2.service;

import com.transit.graphbased_v2.exceptions.BadRequestException;
import com.transit.graphbased_v2.exceptions.ForbiddenException;
import com.transit.graphbased_v2.exceptions.NodeNotFoundException;
import com.transit.graphbased_v2.transferobjects.AccessTransferComponent;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AccessService {

    public Optional<AccessTransferComponent> updateConnection(Set<String> readProperties, Set<String> writeProperties, Set<String> shareReadProperties, Set<String> shareWriteProperties, UUID oId, UUID identityId, UUID requestedById) throws BadRequestException, ForbiddenException;

    public Optional<AccessTransferComponent> getAccess(UUID oId, UUID identityId, UUID requestedById) throws NodeNotFoundException;


    public List<AccessTransferComponent> getAccessList(Set<UUID> objectIds, UUID identityId, UUID requestedById);


    public List<AccessTransferComponent> getAccessClazz(String entityClazz, UUID requestedById, boolean createdByMyOwn, UUID identityId, Integer pagesize);


    public Optional<AccessTransferComponent> createConnection(Set<String> readProperties, Set<String> writeProperties, Set<String> shareReadProperties, Set<String> shareWriteProperties, UUID oId, UUID identityId, UUID requestedById) throws BadRequestException, ForbiddenException;

    public boolean deleteConnectionRecursive(UUID oId, UUID identityId, UUID requestedById);

}
