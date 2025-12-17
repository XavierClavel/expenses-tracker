import {apiClient} from "@/src/api/client";
import SubcategoryIn from "@/src/types/SubcategoryIn";

export async function createSubcategory(subcategory: SubcategoryIn) {
    const res = await apiClient.post("/subcategories", subcategory)
    console.log(res.request.data)
    console.log(res.data)
    return res.data;
}

export async function updateSubcategory(id: number, subcategory: SubcategoryIn) {
    const res = await apiClient.put(`/subcategories/${id}`, subcategory)
    return res.data;
}

export async function deleteSubcategory(id: number) {
    await apiClient.delete(`subcategories/${id}`)
}