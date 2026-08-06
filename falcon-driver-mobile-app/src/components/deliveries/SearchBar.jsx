import { Ionicons } from "@expo/vector-icons";
import { StyleSheet, TextInput, View } from "react-native";

import colors from "../../constants/colors";

export default function SearchBar({ onChangeText, value }) {
  return (
    <View style={styles.container}>
      <Ionicons color={colors.gray} name="search-outline" size={20} />
      <TextInput
        accessibilityLabel="Search deliveries"
        autoCapitalize="none"
        onChangeText={onChangeText}
        placeholder="Search note, customer or destination"
        placeholderTextColor={colors.gray}
        style={styles.input}
        value={value}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { alignItems: "center", backgroundColor: colors.white, borderColor: colors.border, borderRadius: 12, borderWidth: 1, flexDirection: "row", minHeight: 48, paddingHorizontal: 14 },
  input: { color: colors.text, flex: 1, fontSize: 14, marginLeft: 9, paddingVertical: 10 },
});
