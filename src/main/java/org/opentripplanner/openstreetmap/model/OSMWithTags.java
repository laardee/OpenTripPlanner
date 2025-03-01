package org.opentripplanner.openstreetmap.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.openstreetmap.OsmProvider;
import org.opentripplanner.transit.model.basic.Accessibility;

/**
 * A base class for OSM entities containing common methods.
 */

public class OSMWithTags {

  /* To save memory this is only created when an entity actually has tags. */
  private Map<String, String> tags;

  protected long id;

  protected I18NString creativeName;

  private OsmProvider osmProvider;

  static final Set<String> levelTags = Set.of("level", "layer");

  public static boolean isFalse(String tagValue) {
    return ("no".equals(tagValue) || "0".equals(tagValue) || "false".equals(tagValue));
  }

  public static boolean isTrue(String tagValue) {
    return ("yes".equals(tagValue) || "1".equals(tagValue) || "true".equals(tagValue));
  }

  /**
   * Gets the id.
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the id.
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Adds a tag.
   */
  public void addTag(OSMTag tag) {
    if (tags == null) tags = new HashMap<>();

    tags.put(tag.getK().toLowerCase(), tag.getV());
  }

  /**
   * Adds a tag.
   */
  public void addTag(String key, String value) {
    if (key == null || value == null) return;

    if (tags == null) tags = new HashMap<>();

    tags.put(key.toLowerCase(), value);
  }

  /**
   * The tags of an entity.
   */
  public Map<String, String> getTags() {
    return tags;
  }

  /**
   * Is the tag defined?
   */
  public boolean hasTag(String tag) {
    tag = tag.toLowerCase();
    return tags != null && tags.containsKey(tag);
  }

  /**
   * Determines if a tag contains a false value. 'no', 'false', and '0' are considered false.
   */
  public boolean isTagFalse(String tag) {
    tag = tag.toLowerCase();
    if (tags == null) {
      return false;
    }

    return isFalse(getTag(tag));
  }

  /**
   * Returns the level of wheelchair access of the element.
   */
  public Accessibility getWheelchairAccessibility() {
    if (isTagTrue("wheelchair")) {
      return Accessibility.POSSIBLE;
    } else if (isTagFalse("wheelchair")) {
      return Accessibility.NOT_POSSIBLE;
    } else {
      return Accessibility.NO_INFORMATION;
    }
  }

  /**
   * Determines if a tag contains a true value. 'yes', 'true', and '1' are considered true.
   */
  public boolean isTagTrue(String tag) {
    tag = tag.toLowerCase();
    if (tags == null) {
      return false;
    }

    return isTrue(getTag(tag));
  }

  public boolean doesTagAllowAccess(String tag) {
    if (tags == null) {
      return false;
    }
    if (isTagTrue(tag)) {
      return true;
    }
    tag = tag.toLowerCase();
    String value = getTag(tag);
    return (
      "designated".equals(value) ||
      "official".equals(value) ||
      "permissive".equals(value) ||
      "unknown".equals(value)
    );
  }

  /** @return a tag's value, converted to lower case. */
  public String getTag(String tag) {
    tag = tag.toLowerCase();
    if (tags != null && tags.containsKey(tag)) {
      return tags.get(tag);
    }
    return null;
  }

  /**
   * Returns true if both key and value matches.
   */
  public boolean matchesKeyValue(String key, String value) {
    return hasTag(key) && getTag(key).equals(value);
  }

  /**
   * Get tag and convert it to an integer. If the tag exist, but can not be parsed into a number,
   * then the error handler is called with the value witch failed to parse.
   */
  public OptionalInt getTagAsInt(String tag, Consumer<String> errorHandler) {
    String value = getTag(tag);
    if (value != null) {
      try {
        return OptionalInt.of(Integer.parseInt(value));
      } catch (NumberFormatException e) {
        errorHandler.accept(value);
      }
    }
    return OptionalInt.empty();
  }

  /**
   * Checks is a tag contains the specified value.
   */
  public Boolean isTag(String tag, String value) {
    tag = tag.toLowerCase();
    if (tags != null && tags.containsKey(tag) && value != null) {
      return value.equals(tags.get(tag));
    }

    return false;
  }

  /**
   * Returns a name-like value for an entity (if one exists). The otp: namespaced tags are created
   * by {@link OsmModule}
   */
  public I18NString getAssumedName() {
    if (tags == null) {
      return null;
    }
    if (tags.containsKey("name")) {
      return TranslatedString.getI18NString(this.generateI18NForPattern("{name}"), true, false);
    }
    if (tags.containsKey("otp:route_name")) {
      return new NonLocalizedString(tags.get("otp:route_name"));
    }
    if (this.creativeName != null) {
      return this.creativeName;
    }
    if (tags.containsKey("otp:route_ref")) {
      return new NonLocalizedString(tags.get("otp:route_ref"));
    }
    if (tags.containsKey("ref")) {
      return new NonLocalizedString(tags.get("ref"));
    }
    return null;
  }

  /**
   * Replace various pattern by the OSM tag values, with I18n support.
   *
   * @param pattern Pattern containing options tags to replace, such as "text" or "note: {note}".
   *                Tag names between {} are replaced by the OSM tag value, if it is present (or the
   *                empty string if not).
   * @return A map language code → text, with at least one entry for the default language, and any
   * other language found in OSM tag.
   */
  public Map<String, String> generateI18NForPattern(String pattern) {
    if (pattern == null) {
      return null;
    }

    Map<String, StringBuffer> i18n = new HashMap<>();
    i18n.put(null, new StringBuffer());
    Matcher matcher = Pattern.compile("\\{(.*?)}").matcher(pattern);

    int lastEnd = 0;
    while (matcher.find()) {
      // add the stuff before the match
      for (StringBuffer sb : i18n.values()) sb.append(pattern, lastEnd, matcher.start());
      lastEnd = matcher.end();
      // and then the value for the match
      String defKey = matcher.group(1);
      // scan all translated tags
      Map<String, String> i18nTags = getTagsByPrefix(defKey);
      if (i18nTags != null) {
        for (Map.Entry<String, String> kv : i18nTags.entrySet()) {
          if (!kv.getKey().equals(defKey)) {
            String lang = kv.getKey().substring(defKey.length() + 1);
            if (!i18n.containsKey(lang)) i18n.put(lang, new StringBuffer(i18n.get(null)));
          }
        }
      }
      // get the simple value (eg: description=...)
      String defTag = getTag(defKey);
      if (defTag == null && i18nTags != null && i18nTags.size() != 0) {
        defTag = i18nTags.values().iterator().next();
      }
      // get the translated value, if exists
      for (String lang : i18n.keySet()) {
        String i18nTag = getTag(defKey + ":" + lang);
        i18n.get(lang).append(i18nTag != null ? i18nTag : (defTag != null ? defTag : ""));
      }
    }
    for (StringBuffer sb : i18n.values()) sb.append(pattern, lastEnd, pattern.length());
    Map<String, String> out = new HashMap<>(i18n.size());
    for (Map.Entry<String, StringBuffer> kv : i18n.entrySet()) out.put(
      kv.getKey(),
      kv.getValue().toString()
    );
    return out;
  }

  public Map<String, String> getTagsByPrefix(String prefix) {
    Map<String, String> out = new HashMap<>();
    for (Map.Entry<String, String> entry : tags.entrySet()) {
      String k = entry.getKey();
      if (k.equals(prefix) || k.startsWith(prefix + ":")) {
        out.put(k, entry.getValue());
      }
    }
    if (out.isEmpty()) {
      return null;
    }

    return out;
  }

  /**
   * Returns true if this element is under construction.
   */
  public boolean isUnderConstruction() {
    String highway = getTag("highway");
    String cycleway = getTag("cycleway");
    return "construction".equals(highway) || "construction".equals(cycleway);
  }

  /**
   * Returns true if access is generally denied to this element (potentially with exceptions).
   */
  public boolean isGeneralAccessDenied() {
    return isTagDeniedAccess("access");
  }

  /**
   * Returns true if cars are explicitly denied access.
   */
  public boolean isMotorcarExplicitlyDenied() {
    return isTagDeniedAccess("motorcar");
  }

  /**
   * Returns true if cars are explicitly allowed.
   */
  public boolean isMotorcarExplicitlyAllowed() {
    return doesTagAllowAccess("motorcar");
  }

  /**
   * Returns true if cars/motorcycles/HGV are explicitly denied access.
   */
  public boolean isMotorVehicleExplicitlyDenied() {
    return isTagDeniedAccess("motor_vehicle");
  }

  /**
   * Returns true if cars/motorcycles/HGV are explicitly allowed.
   */
  public boolean isMotorVehicleExplicitlyAllowed() {
    return doesTagAllowAccess("motor_vehicle");
  }

  /**
   * Returns true if all land vehicles (including bicycles) are explicitly denied access.
   */
  public boolean isVehicleExplicitlyDenied() {
    return isTagDeniedAccess("vehicle");
  }

  /**
   * Returns true if all land vehicles (including bicycles) are explicitly allowed.
   */
  public boolean isVehicleExplicitlyAllowed() {
    return doesTagAllowAccess("vehicle");
  }

  /**
   * Returns true if bikes are explicitly denied access.
   * <p>
   * bicycle is denied if bicycle:no, bicycle:dismount, bicycle:license or bicycle:use_sidepath
   */
  public boolean isBicycleExplicitlyDenied() {
    return (
      isTagDeniedAccess("bicycle") ||
      "dismount".equals(getTag("bicycle")) ||
      "use_sidepath".equals(getTag("bicycle"))
    );
  }

  /**
   * Returns true if bikes are explicitly allowed.
   */
  public boolean isBicycleExplicitlyAllowed() {
    return doesTagAllowAccess("bicycle");
  }

  /**
   * Returns true if pedestrians are explicitly denied access.
   */
  public boolean isPedestrianExplicitlyDenied() {
    return isTagDeniedAccess("foot");
  }

  /**
   * Returns true if pedestrians are explicitly allowed.
   */
  public boolean isPedestrianExplicitlyAllowed() {
    return doesTagAllowAccess("foot");
  }

  /**
   * @return True if this node / area is a park and ride.
   */
  public boolean isParkAndRide() {
    String parkingType = getTag("parking");
    String parkAndRide = getTag("park_ride");
    return (
      isTag("amenity", "parking") &&
      (
        (parkingType != null && parkingType.contains("park_and_ride")) ||
        (parkAndRide != null && !parkAndRide.equalsIgnoreCase("no"))
      )
    );
  }

  /**
   * Is this a public transport boarding location where passengers wait for transti and that can be
   * linked to a transit stop vertex later on.
   * <p>
   * This intentionally excludes railway=stop and public_transport=stop because these are supposed
   * to be placed on the tracks not on the platform.
   *
   * @return whether the node is a transit stop
   */
  public boolean isBoardingLocation() {
    return (
      "bus_stop".equals(getTag("highway")) ||
      "tram_stop".equals(getTag("railway")) ||
      "station".equals(getTag("railway")) ||
      "halt".equals(getTag("railway")) ||
      "bus_station".equals(getTag("amenity")) ||
      "ferry_terminal".equals(getTag("amenity")) ||
      isPlatform()
    );
  }

  public boolean isPlatform() {
    return "platform".equals(getTag("public_transport")) || "platform".equals(getTag("railway"));
  }

  /**
   * @return True if this entity provides an entrance to a platform or similar entity
   */
  public boolean isEntrance() {
    return (
      (
        isTag("railway", "subway_entrance") ||
        isTag("highway", "elevator") ||
        isTag("entrance", "yes") ||
        isTag("entrance", "main")
      ) &&
      !isTag("access", "private") &&
      !isTag("access", "no")
    );
  }

  /**
   * @return True if this node / area is a bike parking.
   */
  public boolean isBikeParking() {
    return (
      isTag("amenity", "bicycle_parking") && !isTag("access", "private") && !isTag("access", "no")
    );
  }

  public void setCreativeName(I18NString creativeName) {
    this.creativeName = creativeName;
  }

  public String getOpenStreetMapLink() {
    return null;
  }

  /**
   * Returns all non-empty values of the tags passed in as input values.
   * <p>
   * Values are split by semicolons.
   */
  public Set<String> getMultiTagValues(Set<String> refTags) {
    return refTags
      .stream()
      .map(this::getTag)
      .filter(Objects::nonNull)
      .flatMap(v -> Arrays.stream(v.split(";")))
      .map(String::strip)
      .filter(v -> !v.isBlank())
      .collect(Collectors.toSet());
  }

  public OsmProvider getOsmProvider() {
    return osmProvider;
  }

  public void setOsmProvider(OsmProvider provider) {
    this.osmProvider = provider;
  }

  /**
   * Returns true if this tag is explicitly access to this entity.
   */
  private boolean isTagDeniedAccess(String tagName) {
    String tagValue = getTag(tagName);
    return "no".equals(tagValue) || "license".equals(tagValue);
  }

  /**
   * Returns level tag (i.e. building floor) or layer tag values, defaults to "0"
   * Some entities can have a semicolon separated list of levels (e.g. elevators)
   */
  public Set<String> getLevels() {
    var levels = getMultiTagValues(levelTags);
    if (levels.isEmpty()) {
      // default
      return Set.of("0");
    }
    return levels;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("tags", tags).toString();
  }
}
