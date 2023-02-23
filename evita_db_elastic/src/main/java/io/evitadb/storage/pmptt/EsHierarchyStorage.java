/*
 *                         _ _        ____  ____
 *               _____   _(_) |_ __ _|  _ \| __ )
 *              / _ \ \ / / | __/ _` | | | |  _ \
 *             |  __/\ V /| | || (_| | |_| | |_) |
 *              \___| \_/ |_|\__\__,_|____/|____/
 *
 *   Copyright (c) 2023
 *
 *   Licensed under the Business Source License, Version 1.1 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   https://github.com/FgForrest/evitaDB/blob/main/LICENSE
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.evitadb.storage.pmptt;


import lombok.Getter;
import lombok.Setter;
import one.edee.oss.pmptt.dao.HierarchyStorage;
import one.edee.oss.pmptt.model.*;
import one.edee.oss.pmptt.spi.HierarchyChangeListener;
import one.edee.oss.pmptt.util.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class EsHierarchyStorage implements HierarchyStorage {
    private final List<HierarchyChangeListener> changeListeners = new CopyOnWriteArrayList<>();

    @Getter
    @Setter
    private Map<String, HierarchyWithContents> hierarchyIndex = new ConcurrentHashMap<>();

    @Override
    public void registerChangeListener(HierarchyChangeListener listener) {
        this.changeListeners.add(listener);
    }

    @Override
    public void createHierarchy(Hierarchy hierarchy) {
        Assert.isTrue(!hierarchyIndex.containsKey(hierarchy.getCode()), "Hierarchy code " + hierarchy.getCode() + " is not unique!");
        hierarchyIndex.put(hierarchy.getCode(), new HierarchyWithContents(hierarchy));
        hierarchy.setStorage(this);
    }

    public void replaceHierarchy(Hierarchy hierarchy) {
        hierarchyIndex.put(hierarchy.getCode(), new HierarchyWithContents(hierarchy));
        hierarchy.setStorage(this);
    }

    @Override
    public Hierarchy getHierarchy(String code) {
        final HierarchyWithContents hierarchyWithContents = hierarchyIndex.get(code);
        return hierarchyWithContents == null ? null : hierarchyWithContents.getHierarchy();
    }

    @Override
    public Collection<String> getExistingHierarchyCodes() {
        return hierarchyIndex.keySet();
    }

    @Override
    public boolean removeHierarchy(String code) {
        return hierarchyIndex.remove(code) != null;
    }

    @Override
    public void createItem(HierarchyItem newItem, HierarchyItem parent) {
        final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(newItem.getHierarchyCode());

        hierarchyWithContents.addItem(newItem, parent == null ? null : parent.getCode());
        for (HierarchyChangeListener changeListener : changeListeners) {
            changeListener.itemCreated(newItem);
        }
    }

    @Override
    public void updateItem(HierarchyItem updatedItem) {
        final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(updatedItem.getHierarchyCode());
        hierarchyWithContents.updateItem(updatedItem);

        // in memory implementation instances are identities and are already updated
        for (HierarchyChangeListener changeListener : changeListeners) {
            Assert.isTrue(updatedItem instanceof HierarchyItemWithHistory, "Hierarchy item is not of type HierarchyItemWithHistory!");
            final HierarchyItemWithHistory hiwh = (HierarchyItemWithHistory) updatedItem;
            changeListener.itemUpdated(hiwh.getDelegate(), hiwh.getOriginal());
        }
    }

    @Override
    public void removeItem(HierarchyItem removedItem) {
        final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(removedItem.getHierarchyCode());
        hierarchyWithContents.removeItem(removedItem);

        for (HierarchyChangeListener changeListener : changeListeners) {
            changeListener.itemRemoved(removedItem);
        }
    }

    @Override
    public HierarchyItem getItem(String hierarchyCode, String code) {
        final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(hierarchyCode);
        return hierarchyWithContents.getItem(code);
    }

    @Override
    public HierarchyItem getParentItem(HierarchyItem pivot) {
        final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(pivot.getHierarchyCode());
        return hierarchyWithContents.getParentItem(pivot.getCode());
    }

    @Nonnull
    @Override
    public List<HierarchyItem> getParentsOfItem(HierarchyItem pivot) {
        final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(pivot.getHierarchyCode());
        return hierarchyWithContents.getParentItems(pivot.getCode());
    }

    @Nonnull
    @Override
    public List<HierarchyItem> getRootItems(String hierarchyCode) {
        final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(hierarchyCode);
        return hierarchyWithContents.getRootItems();
    }

    @Nonnull
    @Override
    public List<HierarchyItem> getChildItems(HierarchyItem parent) {
        final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(parent.getHierarchyCode());
        return hierarchyWithContents.getChildItems(parent);
    }

    @Nonnull
    @Override
    public List<HierarchyItem> getAllChildrenItems(HierarchyItem parent) {
        final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(parent.getHierarchyCode());
        return hierarchyWithContents.getAllChildItems(parent);
    }

    @Nonnull
    @Override
    public List<HierarchyItem> getLeafItems(HierarchyItem parent) {
        final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(parent.getHierarchyCode());
        return hierarchyWithContents.getAllLeafItems(parent);
    }

    @Nonnull
    @Override
    public List<HierarchyItem> getLeafItems(String hierarchyCode) {
        final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(hierarchyCode);
        return hierarchyWithContents.getAllLeafItems(null);
    }

    @Nullable
    @Override
    public SectionWithBucket getFirstEmptySection(String hierarchyCode, long sectionSize, short maxCount) {
        final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(hierarchyCode);
        final List<HierarchyItem> rootItemsByLeftBound = hierarchyWithContents.getRootItemsByLeftBound();
        final long initialLeftBound = 1L;
        return getFirstEmptySection(sectionSize, maxCount, rootItemsByLeftBound, initialLeftBound);
    }

    @Nullable
    @Override
    public SectionWithBucket getFirstEmptySection(String hierarchyCode, long sectionSize, short maxCount, HierarchyItem parent) {
        final HierarchyWithContents hierarchyWithContents = getHierarchyWithContents(hierarchyCode);
        final List<HierarchyItem> childItemsByLeftBound = hierarchyWithContents.getChildItemsByLeftBound(parent);
        return getFirstEmptySection(sectionSize, maxCount, childItemsByLeftBound, parent.getLeftBound() + 1L);
    }

    private HierarchyWithContents getHierarchyWithContents(String hierarchyCode) {
        final HierarchyWithContents hierarchyWithContents = hierarchyIndex.get(hierarchyCode);
        Assert.notNull(hierarchyWithContents, "Hierarchy with code " + hierarchyCode + " not found!");
        return hierarchyWithContents;
    }

    private SectionWithBucket getFirstEmptySection(long sectionSize, short maxCount, List<HierarchyItem> items, long initialLeftBound) {
        if (items.size() + 1 >= maxCount) {
            return null;
        }
        if (items.isEmpty()) {
            return new SectionWithBucket(initialLeftBound, initialLeftBound + sectionSize - 1, (short) 1);
        } else {
            Long lastLeftBound = null;
            for (int i = 0; i < items.size(); i++) {
                final HierarchyItem item = items.get(i);
                if (lastLeftBound == null) {
                    if (item.getLeftBound() > initialLeftBound) {
                        return new SectionWithBucket(
                                initialLeftBound,
                                initialLeftBound + sectionSize - 1,
                                (short) 1
                        );
                    }
                } else {
                    if (item.getLeftBound() > lastLeftBound + sectionSize) {
                        return new SectionWithBucket(
                                lastLeftBound + sectionSize,
                                lastLeftBound + 2 * sectionSize - 1,
                                (short) (i + 1)
                        );
                    }
                }
                lastLeftBound = item.getLeftBound();
            }
            return new SectionWithBucket(
                    lastLeftBound + sectionSize,
                    lastLeftBound + sectionSize * 2 - 1,
                    (short) (items.size() + 1)
            );
        }
    }

    public static class HierarchyWithContents {
        private static final String ROOT_LEVEL = "__root";
        @Getter
        private final Hierarchy hierarchy;
        private final Map<String, HierarchyLevel> levels = new HashMap<>();
        private final Map<String, HierarchyLevel> itemParents = new HashMap<>();

        public HierarchyWithContents(Hierarchy hierarchy) {
            this.hierarchy = hierarchy;
            final Section rootSection = Section.computeEntireHierarchyBounds(hierarchy.getSectionSize(), hierarchy.getLevels());
            final HierarchyItem rootItem = new HierarchyItemWithHistory(hierarchy.getCode(), ROOT_LEVEL, (short) 0, rootSection.getLeftBound(), rootSection.getRightBound(), (short) 1);
            this.levels.put(ROOT_LEVEL, new HierarchyLevel(rootItem));
        }

        List<HierarchyItem> getAllLeafItems(HierarchyItem withParent) {
            final LinkedList<HierarchyItem> result = new LinkedList<>();
            final List<HierarchyItem> itemsToGoThrough = withParent == null ? levels.get(ROOT_LEVEL).getChildren() : levels.get(withParent.getCode()).getChildren();
            addLeafItems(result, itemsToGoThrough);
            return result;
        }

        private void addLeafItems(LinkedList<HierarchyItem> result, List<HierarchyItem> itemsToGoThrough) {
            for (HierarchyItem item : itemsToGoThrough) {
                if (item.getNumberOfChildren() == 0) {
                    result.add(item);
                }
                final HierarchyLevel level = levels.get(item.getCode());
                if (!level.getChildren().isEmpty()) {
                    addLeafItems(result, level.getChildren());
                }
            }
        }

        List<HierarchyItem> getParentItems(String code) {
            final LinkedList<HierarchyItem> result = new LinkedList<>();
            String lookUpCode = code;
            HierarchyLevel level;
            do {
                level = itemParents.get(lookUpCode);
                if (!ROOT_LEVEL.equals(level.getItem().getCode())) {
                    lookUpCode = level.getItem().getCode();
                    result.add(level.getItem());
                }
            } while (!ROOT_LEVEL.equals(level.getItem().getCode()));
            Collections.reverse(result);
            return result;
        }

        List<HierarchyItem> getRootItems() {
            final List<HierarchyItem> result = new ArrayList<>(levels.get(ROOT_LEVEL).getChildren());
            result.sort(HierarchyItemOrderComparator.INSTANCE);
            return result;
        }

        List<HierarchyItem> getRootItemsByLeftBound() {
            final List<HierarchyItem> result = new ArrayList<>(levels.get(ROOT_LEVEL).getChildren());
            result.sort(HierarchyItemLeftBoundComparator.INSTANCE);
            return result;
        }

        List<HierarchyItem> getChildItems(HierarchyItem parent) {
            final List<HierarchyItem> result = new ArrayList<>(levels.get(parent.getCode()).getChildren());
            result.sort(HierarchyItemOrderComparator.INSTANCE);
            return result;
        }

        List<HierarchyItem> getChildItemsByLeftBound(HierarchyItem parent) {
            final List<HierarchyItem> result = new ArrayList<>(levels.get(parent.getCode()).getChildren());
            result.removeIf(child -> !(child.getLeftBound() >= parent.getLeftBound() && child.getRightBound() <= parent.getRightBound()));
            result.sort(HierarchyItemLeftBoundComparator.INSTANCE);
            return result;
        }

        List<HierarchyItem> getAllChildItems(HierarchyItem parent) {
            final List<HierarchyItem> result = new LinkedList<>();
            addChildren(parent, result);
            return result;
        }

        HierarchyItem getParentItem(String code) {
            final HierarchyLevel hierarchyLevel = itemParents.get(code);
            return ROOT_LEVEL.equals(hierarchyLevel.getItem().getCode()) ? null : hierarchyLevel.getItem();
        }

        HierarchyItem getItem(String code) {
            final HierarchyLevel hierarchyLevel = itemParents.get(code);
            if (hierarchyLevel != null) {
                for (HierarchyItem child : hierarchyLevel.getChildren()) {
                    if (Objects.equals(code, child.getCode())) {
                        return child;
                    }
                }
            }
            return null;
        }

        void addItem(HierarchyItem item, String parent) {
            final HierarchyLevel level = levels.get(parent == null ? ROOT_LEVEL : parent);
            level.getChildren().add(item);
            itemParents.put(item.getCode(), level);
            levels.put(item.getCode(), new HierarchyLevel(item));
        }

        void updateItem(HierarchyItem updatedItem) {
            final Section parentSection = Section.computeParentSectionBounds(hierarchy.getSectionSize(), updatedItem);
            for (HierarchyLevel level : levels.values()) {
                if (level.getItem().getLeftBound().equals(parentSection.getLeftBound()) && level.getItem().getRightBound().equals(parentSection.getRightBound()) && !level.getChildren().contains(updatedItem)) {
                    level.getChildren().add(updatedItem);
                    final HierarchyLevel oldParent = itemParents.get(updatedItem.getCode());
                    if (oldParent != null) {
                        oldParent.getChildren().remove(updatedItem);
                    }
                    itemParents.put(updatedItem.getCode(), level);
                }
            }
        }

        void removeItem(HierarchyItem item) {
            final HierarchyLevel hierarchyLevel = itemParents.remove(item.getCode());
            hierarchyLevel.getChildren().remove(item);
            levels.remove(item.getCode());
        }

        private void addChildren(HierarchyItem parent, List<HierarchyItem> result) {
            final List<HierarchyItem> children = new ArrayList<>(levels.get(parent.getCode()).getChildren());
            children.sort(HierarchyItemOrderComparator.INSTANCE);
            result.addAll(children);

            for (HierarchyItem child : children) {
                addChildren(child, result);
            }
        }

    }

    private static class HierarchyItemOrderComparator implements Comparator<HierarchyItem>, Serializable {
        private static final HierarchyItemOrderComparator INSTANCE = new HierarchyItemOrderComparator();
        private static final long serialVersionUID = 3021563606387314468L;

        @Override
        public int compare(HierarchyItem o1, HierarchyItem o2) {
            return Short.compare(o1.getOrder(), o2.getOrder());
        }
    }

    private static class HierarchyItemLeftBoundComparator implements Comparator<HierarchyItem>, Serializable {
        private static final HierarchyItemLeftBoundComparator INSTANCE = new HierarchyItemLeftBoundComparator();
        private static final long serialVersionUID = -8262193400044256075L;

        @Override
        public int compare(HierarchyItem o1, HierarchyItem o2) {
            return Long.compare(o1.getLeftBound(), o2.getLeftBound());
        }
    }

}
