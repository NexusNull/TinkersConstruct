package tconstruct.library.tools;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import mantle.items.abstracts.CraftingItem;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import tconstruct.library.TConstructRegistry;
import tconstruct.library.util.IToolPart;
import tconstruct.library.util.TextureHelper;
import tconstruct.util.config.PHConstruct;

public class DynamicToolPart extends CraftingItem implements IToolPart {
    public String partName;
    public String texture;
    public IIcon defaultIcon;
    public Class<? extends CustomMaterial> customMaterialClass;

    private boolean hidden = false;

    public DynamicToolPart(String texture, String name) {
        this(texture, name, (Class<? extends CustomMaterial>) null);
    }

    public DynamicToolPart(String texture, String name, Class<? extends CustomMaterial> customMaterialClass) {
        this(texture, name, "tinker", customMaterialClass);
    }

    public DynamicToolPart(String texture, String name, String domain) {
        this(texture, name, domain, (Class<? extends CustomMaterial>) null);
    }

    public DynamicToolPart(
            String texture, String name, String domain, Class<? extends CustomMaterial> customMaterialClass) {
        super(null, null, "parts/", domain, TConstructRegistry.partTab);
        this.setUnlocalizedName("tconstruct." + name);
        this.partName = name;
        this.texture = texture;
        this.customMaterialClass = customMaterialClass;
    }

    /**
     * Doesn't add the item to creative tabs
     */
    public DynamicToolPart hide() {
        hidden = true;

        return this;
    }

    // item meta = material id
    @Override
    public int getMaterialID(ItemStack stack) {
        if (TConstructRegistry.toolMaterials.keySet().contains(stack.getItemDamage())) return stack.getItemDamage();

        return -1;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String material = "";
        String matName = "";
        if (customMaterialClass == null) {
            tconstruct.library.tools.ToolMaterial toolmat = TConstructRegistry.getMaterial(getMaterialID(stack));
            if (toolmat == null) return super.getItemStackDisplayName(stack);

            material = toolmat.localizationString.toLowerCase(Locale.ENGLISH).startsWith("material.")
                    ? toolmat.localizationString.substring(9)
                    : toolmat.localizationString; // :(
            matName = toolmat.prefixName();
        } else {
            CustomMaterial customMaterial =
                    TConstructRegistry.getCustomMaterial(getMaterialID(stack), customMaterialClass);
            if (customMaterial == null) return super.getItemStackDisplayName(stack);

            material = "";
            if (customMaterial.input != null) {
                material = customMaterial.input.getUnlocalizedName();
                int firstPeriodIndex = material.indexOf('.');
                if (firstPeriodIndex >= 0) material = material.substring(firstPeriodIndex + 1);
                matName = customMaterial.input.getDisplayName();
            } else {
                material = customMaterial.oredict;
                matName = customMaterial.oredict;
            }
        }

        // custom name
        if (StatCollector.canTranslate("toolpart." + partName + "." + material)) {
            return StatCollector.translateToLocal("toolpart." + partName + "." + material);
        }
        // general name
        else {
            // specific material name for materials?
            if (StatCollector.canTranslate("toolpart.material." + material))
                matName = StatCollector.translateToLocal("toolpart.material." + material);

            return StatCollector.translateToLocal("toolpart." + partName).replaceAll("%%material", matName);
        }
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int id = getMaterialID(stack);
        if (id == -1) return getUnlocalizedName();

        String material = "unknown";
        if (customMaterialClass == null) {
            tconstruct.library.tools.ToolMaterial toolmat = TConstructRegistry.getMaterial(getMaterialID(stack));
            material = toolmat.materialName;
        } else {
            CustomMaterial customMaterial =
                    TConstructRegistry.getCustomMaterial(getMaterialID(stack), customMaterialClass);
            material =
                    customMaterial.input != null ? customMaterial.input.getUnlocalizedName() : customMaterial.oredict;
        }

        return "toolpart." + partName + "." + material;
    }

    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        if (hidden) return;

        // material id == metadata
        for (Integer matID : TConstructRegistry.defaultToolPartMaterials) {
            ItemStack stack = new ItemStack(item, 1, matID);
            if (this.getMaterialID(stack) != -1) list.add(stack);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister iconRegister) {
        // get the biggest material index
        int max = -1;
        for (Integer id : TConstructRegistry.toolMaterials.keySet()) if (id > max) max = id;

        this.icons = new IIcon[max + 1];

        // register icon for each material that has one
        if (!PHConstruct.minimalTextures)
            for (Map.Entry<Integer, tconstruct.library.tools.ToolMaterial> entry :
                    TConstructRegistry.toolMaterials.entrySet()) {
                String tex = modTexPrefix + ":" + folder
                        + entry.getValue().materialName.toLowerCase() + texture;
                if (TextureHelper.itemTextureExists(tex)) this.icons[entry.getKey()] = iconRegister.registerIcon(tex);
            }

        // default texture
        this.defaultIcon = iconRegister.registerIcon(modTexPrefix + ":" + folder + texture);
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int meta) {
        if (meta > icons.length) return defaultIcon;

        if (icons[meta] == null) return defaultIcon;

        return icons[meta];
    }

    @Override
    public int getColorFromItemStack(ItemStack stack, int renderpass) {
        int matId = getMaterialID(stack);
        if (matId > icons.length) return super.getColorFromItemStack(stack, renderpass);

        if (matId >= 0 && icons[matId] == null) {
            if (customMaterialClass == null)
                return TConstructRegistry.getMaterial(getMaterialID(stack)).primaryColor();
            else return TConstructRegistry.getCustomMaterial(getMaterialID(stack), customMaterialClass).color;
        }

        return super.getColorFromItemStack(stack, renderpass);
    }
}
