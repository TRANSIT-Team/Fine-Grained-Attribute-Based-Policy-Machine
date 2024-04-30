package com.transit.graphbased_v2.service.impl;

import com.transit.graphbased_v2.domain.graph.nodes.ClazzType;
import com.transit.graphbased_v2.domain.graph.nodes.ObjectAttributeClazz;
import com.transit.graphbased_v2.domain.graph.nodes.ObjectAttributeExtendedClazz;
import com.transit.graphbased_v2.domain.graph.relationships.Relationship;
import com.transit.graphbased_v2.exceptions.BadRequestException;
import com.transit.graphbased_v2.exceptions.ForbiddenException;
import com.transit.graphbased_v2.exceptions.NodeNotFoundException;
import com.transit.graphbased_v2.performacelogging.LogExecutionTime;
import com.transit.graphbased_v2.repository.*;
import com.transit.graphbased_v2.service.AccessService;
import com.transit.graphbased_v2.service.helper.AccessTransferComponentHelper;
import com.transit.graphbased_v2.service.helper.UpdateRightsRecursive;
import com.transit.graphbased_v2.transferobjects.AccessTransferComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.transit.graphbased_v2.common.RightsConstants.*;

@Service
@Slf4j
public class AccessServiceBean implements AccessService {

    @Autowired
    private RightsRepository rightsRepository;

    @Autowired
    private IdentityClazzRepository identityClazzRepository;

    @Autowired
    private ObjectClazzRepository objectClazzRepository;

    @Autowired
    private ObjectAttributeClazzRepository objectAttributeClazzRepository;

    @Autowired
    private RightsConnectionRepository rightsConnectionRepository;

    @Autowired
    private EntityConnectionRepository entityConnectionRepository;

    @Autowired
    private RelationshipConnectionRepository relationshipConnectionRepository;


    @Autowired
    private AssigmentRepository assigmentRepository;

    @Autowired
    private EntityClazzRepository entityClazzRepository;

    @Autowired
    private UpdateRightsRecursive updateRecursive;

    @Autowired
    private AccessTransferComponentHelper accessTransferComponentHelper;

    @Override
    public Optional<AccessTransferComponent> updateConnection(Set<String> readProperties, Set<String> writeProperties, Set<String> shareReadProperties, Set<String> shareWriteProperties, UUID oId, UUID identityId, UUID requestedById) throws BadRequestException, ForbiddenException {

        if (readProperties == null) {
            readProperties = new HashSet<>();
        }
        if (writeProperties == null) {
            writeProperties = new HashSet<>();
        }
        if (shareReadProperties == null) {
            shareReadProperties = new HashSet<>();
        }
        if (shareWriteProperties == null) {
            shareWriteProperties = new HashSet<>();
        }

        if (readProperties.size() < writeProperties.size()) {
            throw new BadRequestException("Cannot have more write Properties as read Properties.");
        }

        if (readProperties.size() < shareReadProperties.size()) {
            throw new BadRequestException("Cannot have more sharing read Properties as read Properties.");
        }

        if (writeProperties.size() < shareWriteProperties.size()) {
            throw new BadRequestException("Cannot have more sharing write Properties as write Properties.");
        }

        var objectList = rightsRepository.checkIfAllNodesExistsForAccess(identityId, requestedById, oId);

        if (objectList.isEmpty()) {
            throw new NodeNotFoundException("Some error occurred.");
        }

        var objects = objectList.get();
        var objectNode = objects.get(0);
        var requestingIdentity = objects.get(1);
        var updatingIdentity = objects.get(2);
        var requestingIdentityRights = objects.get(3);
        var updatingIdentityRights = objects.get(4);

        if (objectNode.getId() == null) {
            throw new NodeNotFoundException("Object not exists");
        }

        if (requestingIdentity.getId() == null) {
            throw new NodeNotFoundException("Identity " + requestedById + " not exists.");
        }

        if (updatingIdentity.getId() == null) {
            throw new NodeNotFoundException("Identity " + identityId + " not exists.");
        }

        if (requestingIdentityRights.getId() == null) {
            throw new BadRequestException("No Access");
        }

        if (requestingIdentityRights.getId() == null) {
            throw new BadRequestException("No Access");
        }
        if (updatingIdentityRights.getId() == null) {
            throw new BadRequestException("Cannot find existing access data for this identity.");
        }


        var currentOa = updatingIdentityRights;

        var oaNode = new ObjectAttributeClazz();

        oaNode.setId(currentOa.getId());
        oaNode.setLabels(currentOa.getLabels());
        oaNode.setEntityClass(currentOa.getEntityClass());
        oaNode.setType(currentOa.getType());
        oaNode.setName(currentOa.getName());

        oaNode.setReadProperties(currentOa.getReadProperties());
        oaNode.setWriteProperties(currentOa.getWriteProperties());
        oaNode.setShareReadProperties(currentOa.getShareReadProperties());
        oaNode.setShareWriteProperties(currentOa.getShareWriteProperties());

        //new version
        ObjectAttributeClazz validationOaNode = validateRightsV2(readProperties, writeProperties, shareReadProperties, shareWriteProperties, requestingIdentityRights);
        oaNode.setReadProperties(validationOaNode.getReadProperties());
        oaNode.setWriteProperties(validationOaNode.getWriteProperties());
        oaNode.setShareReadProperties(validationOaNode.getShareReadProperties());
        oaNode.setShareWriteProperties(validationOaNode.getShareWriteProperties());

        var updatedOa = rightsRepository.updateOa(oaNode);

        // objectAttributeClazzRepository.save(oaNode);
        updateRecursive.updateRecursive(oaNode.getId());


        return updatedOa.map(entry -> accessTransferComponentHelper.getAccessTransferComponent(entry.getObjectAttributeClazz(), entry.getOId(), entry.getOEntityClazz(), identityId));

    }

    @LogExecutionTime
    @Override
    public Optional<AccessTransferComponent> getAccess(UUID oId, UUID identityId, UUID requestedById) throws NodeNotFoundException {


        var rights = rightsRepository.getRights(identityId, requestedById, oId);

        if (rights.isEmpty()) {
            throw new NodeNotFoundException(oId);
        }

        return rights.map(entry -> accessTransferComponentHelper.getAccessTransferComponent(entry.getObjectAttributeClazz(), entry.getOId(), entry.getOEntityClazz(), identityId));

    }


    @Override
    public List<AccessTransferComponent> getAccessList(Set<UUID> objectIds, UUID identityId, UUID requestedById) {
        return rightsRepository.getRightsListOld(objectIds, identityId, requestedById).stream().map(entry -> accessTransferComponentHelper.getAccessTransferComponent(entry.getObjectAttributeClazz(), entry.getOId(), entry.getOEntityClazz(), identityId)).collect(Collectors.toList());
    }

    @Override
    public List<AccessTransferComponent> getAccessClazz(String entityClazz, UUID requestedById, boolean createdByMyOwn, UUID identityId, Integer pagesize) {


        return rightsRepository.getRightsClass(entityClazz, requestedById, createdByMyOwn, identityId, pagesize).stream().map(entry -> {

            if (identityId == null) {
                return accessTransferComponentHelper.getAccessTransferComponent(entry.getObjectAttributeClazz(), entry.getOId(), entry.getOEntityClazz(), requestedById);
            } else {
                return accessTransferComponentHelper.getAccessTransferComponent(entry.getObjectAttributeClazz(), entry.getOId(), entry.getOEntityClazz(), identityId);
            }
        }).collect(Collectors.toList());
    }

    @LogExecutionTime
    @Override
    public Optional<AccessTransferComponent> createConnection(Set<String> readProperties, Set<String> writeProperties, Set<String> shareReadProperties, Set<String> shareWriteProperties, UUID oId, UUID identityId, UUID requestedById) throws BadRequestException, ForbiddenException {

        if (readProperties == null) {
            readProperties = new HashSet<>();
        }
        if (writeProperties == null) {
            writeProperties = new HashSet<>();
        }
        if (shareReadProperties == null) {
            shareReadProperties = new HashSet<>();
        }
        if (shareWriteProperties == null) {
            shareWriteProperties = new HashSet<>();
        }

        if (readProperties.size() < writeProperties.size()) {
            throw new BadRequestException("Cannot have more write Properties as read Properties.");
        }

        if (readProperties.size() < shareReadProperties.size()) {
            throw new BadRequestException("Cannot have more sharing read Properties as read Properties.");
        }

        if (writeProperties.size() < shareWriteProperties.size()) {
            throw new BadRequestException("Cannot have more sharing write Properties as write Properties.");
        }

        var objectList = rightsRepository.checkIfAllNodesExistsForAccess(identityId, requestedById, oId);

        if (objectList.isEmpty()) {
            throw new NodeNotFoundException("Some error occurred.");
        }

        var objects = objectList.get();
        var objectNode = objects.get(0);
        var requestingIdentity = objects.get(1);
        var updatingIdentity = objects.get(2);
        var requestingIdentityRights = objects.get(3);
        var updatingIdentityRights = objects.get(4);

        if (objectNode.getId() == null) {
            throw new NodeNotFoundException("Object not exists");
        }

        if (requestingIdentity.getId() == null) {
            throw new NodeNotFoundException("Identity " + requestedById + " not exists.");
        }

        if (updatingIdentity.getId() == null) {
            throw new NodeNotFoundException("Identity " + identityId + " not exists.");
        }

        if (requestingIdentityRights.getId() == null) {
            throw new BadRequestException("No Access");
        }

        if (requestingIdentityRights.getId() == null) {
            throw new BadRequestException("No Access");
        }
        if (updatingIdentityRights.getId() != null) {
            throw new BadRequestException("Rights already exists--> Have to update not create.");
        }

        ObjectAttributeClazz oaNode = new ObjectAttributeClazz();
        ObjectAttributeClazz validationOaNode = validateRightsV2(readProperties, writeProperties, shareReadProperties, shareWriteProperties, requestingIdentityRights);

        oaNode.setId(UUID.randomUUID());
        oaNode.setName("OA#" + oaNode.getId() + "Group#" + identityId + "#" + oId);
        oaNode.setType(ClazzType.OA);
        oaNode.setEntityClass("OA");
        oaNode.setReadProperties(validationOaNode.getReadProperties());
        oaNode.setWriteProperties(validationOaNode.getWriteProperties());
        oaNode.setShareReadProperties(validationOaNode.getShareReadProperties());
        oaNode.setShareWriteProperties(validationOaNode.getShareWriteProperties());

        var newOA = rightsRepository.createAccess(oaNode, requestingIdentityRights.getId(), identityId, requestedById, oId);

        return newOA.map(entry -> accessTransferComponentHelper.getAccessTransferComponent(entry.getObjectAttributeClazz(), entry.getOId(), entry.getOEntityClazz(), identityId));
    }


    @Override
    public boolean deleteConnectionRecursive(UUID oId, UUID identityId, UUID requestedById) {
        var rights = rightsRepository.getRights(identityId, requestedById, oId);
        if (rights.isEmpty()) {
            return false;
        }
        var oaId = rights.get().getId();

        long sizeList = 0;

        var connectedOAnodes = new HashSet<UUID>();
        connectedOAnodes.add(rights.get().getId());

        while (sizeList < connectedOAnodes.size()) {
            sizeList = connectedOAnodes.size();
            connectedOAnodes.forEach(id -> {
                connectedOAnodes.addAll(rightsConnectionRepository.getIncomingRelationships(id).stream().map(Relationship::getSourceID).toList());
            });
        }
        deleteOaNodes(connectedOAnodes);
        return true;
    }

    public void deleteOaNodes(HashSet<UUID> connectedOAnodes) {
        while (!connectedOAnodes.isEmpty()) {
            connectedOAnodes.forEach(id -> {
                if (rightsConnectionRepository.getIncomingRelationships(id).isEmpty()) {
                    relationshipConnectionRepository.getIncomingRelationships(id).forEach(assoc -> relationshipConnectionRepository.deleteRelationship(assoc));
                    assigmentRepository.getIncomingRelationships(id).forEach(assign -> assigmentRepository.deleteRelationship(assign));
                    rightsConnectionRepository.getOutgoingRelationships(id).forEach(assoc -> rightsConnectionRepository.deleteRelationship(assoc));
                    objectAttributeClazzRepository.deleteById(id);
                    connectedOAnodes.remove(id);
                }

            });
        }


    }

    private Set<String> validateRights(Set<String> properties, Set<String> restrictionProperties, AccessTransferComponent comp, String propertyType) {

        //comp has the my rights properties (read,write,share), these are the maximum I can edit otherwise I have to edit the Object-Properties via object-endpoint


        //properties have the new properties to update
        //validation that writeProperties can just be the readProperties or less
        //validation that shareReadProperties can just be readProperties or less
        //validation that shareWriteProperties can just be writeProperties or less

        //the properties validation is based on the restriction by the restrictionProperties

        Set<String> filteredProperties = new HashSet<>();

        if (propertyType.equals(READ_PROPERTIES)) {
            filteredProperties.addAll(properties);
            filteredProperties.retainAll(comp.getReadProperties());
            filteredProperties.retainAll(restrictionProperties);
        } else if (propertyType.equals(WRITE_PROPERTIES)) {
            filteredProperties.addAll(properties);
            filteredProperties.retainAll(comp.getWriteProperties());
            filteredProperties.retainAll(restrictionProperties);
        } else if (propertyType.equals(SHARE_READ_PROPERTIES)) {
            filteredProperties.addAll(properties);
            filteredProperties.retainAll(comp.getShareReadProperties());
            filteredProperties.retainAll(restrictionProperties);
        } else if (propertyType.equals(SHARE_WRITE_PROPERTIES)) {
            filteredProperties.addAll(properties);
            filteredProperties.retainAll(comp.getShareWriteProperties());
            filteredProperties.retainAll(restrictionProperties);
        }
        return filteredProperties;
    }

    private ObjectAttributeClazz validateRightsV2(Set<String> givingReadProperties, Set<String> givingWriteProperties, Set<String> givingShareReadProperties, Set<String> givingShareWriteProperties, ObjectAttributeExtendedClazz myRights) {
        // myRights included read, write, share rights which I have
        // givingProperties are the access/rights which I want to give someone

        //####Valditation Rules######
        //properties have the new properties to update
        //validation that writeProperties can just be the readProperties or less
        //validation that shareReadProperties can just be readProperties or less
        //validation that shareWriteProperties can just be writeProperties or less

        //validation that readProperties can only be my shareReadProperties (I can only give readProperties which are allowed for me to share)
        //validation that writeProperties can only be my writeReadProperties (I can only give writeProperties which are allowed for me to share)


        ObjectAttributeClazz validatedOaNode = new ObjectAttributeClazz();
        Set<String> filteredProperties;

        //readProperties
        filteredProperties = new HashSet<>();
        filteredProperties.addAll(givingReadProperties);
        //remove all which are not in my readProperties
        filteredProperties.retainAll(myRights.getReadProperties());
        //remove all which are not in my shareReadProperties
        filteredProperties.retainAll(myRights.getShareReadProperties());
        validatedOaNode.setReadProperties(filteredProperties);

        //writeProperties
        filteredProperties = new HashSet<>();
        filteredProperties.addAll(givingWriteProperties);
        //remove all which are not in givingReadProperties (because I can't write what I can't read)
        filteredProperties.retainAll(givingReadProperties);
        //remove all which are not in my writeProperties
        filteredProperties.retainAll(myRights.getWriteProperties());
        //remove all which are not in my shareWriteProperties
        filteredProperties.retainAll(myRights.getShareWriteProperties());
        validatedOaNode.setWriteProperties(filteredProperties);

        //shareReadProperties
        filteredProperties = new HashSet<>();
        filteredProperties.addAll(givingShareReadProperties);
        //remove all which are not in the givingReadProperties (because I can't allow more to share than I give to read)
        filteredProperties.retainAll(givingReadProperties);
        //remove all which are not in my shareReadProperties
        filteredProperties.retainAll(myRights.getShareReadProperties());
        validatedOaNode.setShareReadProperties(filteredProperties);

        //shareWriteProperties
        filteredProperties = new HashSet<>();
        filteredProperties.addAll(givingShareWriteProperties);
        //remove all which are not in the givingWriteProperties (because I can't allow more to share than I give to write)
        filteredProperties.retainAll(givingWriteProperties);
        //remove all which are not in my shareReadProperties
        filteredProperties.retainAll(myRights.getShareReadProperties());
        //remove all which are not in my shareWriteProperties
        filteredProperties.retainAll(myRights.getShareWriteProperties());
        //remove all which are not in givingShareReadProperties (because I can't forgive other writeProperties than readProperties)
        filteredProperties.retainAll(givingShareReadProperties);

        validatedOaNode.setShareWriteProperties(filteredProperties);

        return validatedOaNode;
    }


}
