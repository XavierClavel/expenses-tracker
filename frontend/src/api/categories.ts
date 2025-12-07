import {apiClient} from "@/src/api/client";
import CategoryOut from "@/src/types/CategoryOut";
import SubcategoryOut from "@/src/types/SubcategoryOut";
import CategoryIn from "@/src/types/CategoryIn";


export async function listCategories(): Promise<CategoryOut[]> {
    const response = await apiClient.get( `/categories`);

    const categories: CategoryOut[] = response.data.map((e: any) =>
        new CategoryOut(
            e.id,
            e.name,
            e.color,
            e.icon,
            e.type,
            e.subcategories.map ((subcategory: any) => {
                return new SubcategoryOut(
                    subcategory.id,
                    subcategory.name,
                    subcategory.type,
                    subcategory.color,
                    subcategory.icon,
                    subcategory.isDefault,
                )
            })
        )
    );

    return categories
}

export async function createCategory(category: CategoryIn) {
    const res = await apiClient.post("/categories", category)
    console.log(res.request.data)
    console.log(res.data)
    return res.data;
}

export async function updateCategory(id: number, category: CategoryIn) {
    const res = await apiClient.put(`/categories/${id}`, category)
    return res.data;
}