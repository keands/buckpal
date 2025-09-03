import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { apiClient } from '@/lib/api';
import type { Category } from '@/types/api';

const categorySchema = z.object({
  name: z.string().min(1, "Le nom est requis").max(100, "Le nom est trop long"),
  description: z.string().max(200, "La description est trop longue").optional(),
  iconName: z.string().optional(),
  colorCode: z.string().optional(),
});

type CategoryFormData = z.infer<typeof categorySchema>;

interface CategoryManagementModalProps {
  isOpen: boolean;
  onClose: () => void;
  category?: Category | null; // null for create, Category for edit
  onSave: (category: Category) => void;
}

export function CategoryManagementModal({
  isOpen,
  onClose,
  category,
  onSave
}: CategoryManagementModalProps) {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [availableIcons, setAvailableIcons] = useState<string[]>([]);
  const [availableColors, setAvailableColors] = useState<string[]>([]);

  const {
    register,
    handleSubmit,
    formState: { errors },
    setValue,
    watch,
    reset
  } = useForm<CategoryFormData>({
    resolver: zodResolver(categorySchema),
    defaultValues: {
      name: '',
      description: '',
      iconName: '📦',
      colorCode: '#6366f1'
    }
  });

  const selectedIcon = watch('iconName');
  const selectedColor = watch('colorCode');

  // Load icons and colors on mount
  useEffect(() => {
    const loadOptions = async () => {
      try {
        const [icons, colors] = await Promise.all([
          apiClient.getAvailableIcons(),
          apiClient.getAvailableColors()
        ]);
        setAvailableIcons(icons);
        setAvailableColors(colors);
      } catch (error) {
        console.error('Failed to load category options:', error);
      }
    };

    if (isOpen) {
      loadOptions();
    }
  }, [isOpen]);

  // Initialize form when category changes
  useEffect(() => {
    if (isOpen) {
      if (category) {
        // Edit mode
        reset({
          name: category.name,
          description: category.description || '',
          iconName: category.iconName || '📦',
          colorCode: category.colorCode || '#6366f1'
        });
      } else {
        // Create mode
        reset({
          name: '',
          description: '',
          iconName: '📦',
          colorCode: '#6366f1'
        });
      }
    }
  }, [category, isOpen, reset]);

  const onSubmit = async (data: CategoryFormData) => {
    try {
      setLoading(true);

      let savedCategory: Category;
      
      if (category) {
        // Edit existing category
        savedCategory = await apiClient.updateCategory(category.id, data);
      } else {
        // Create new category
        savedCategory = await apiClient.createCategory(data);
      }

      onSave(savedCategory);
      onClose();
    } catch (error) {
      console.error('Failed to save category:', error);
      // TODO: Show error toast
    } finally {
      setLoading(false);
    }
  };

  const isEditMode = !!category;
  const title = isEditMode ? 'Modifier la catégorie' : 'Créer une nouvelle catégorie';

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <div className="grid grid-cols-1 gap-4">
            {/* Name Field */}
            <div className="space-y-2">
              <Label htmlFor="name">Nom de la catégorie *</Label>
              <Input
                id="name"
                placeholder="Ex: Transport personnel, Loisirs créatifs..."
                {...register('name')}
                className={errors.name ? 'border-red-500' : ''}
              />
              {errors.name && (
                <p className="text-sm text-red-500">{errors.name.message}</p>
              )}
            </div>

            {/* Description Field */}
            <div className="space-y-2">
              <Label htmlFor="description">Description (optionnel)</Label>
              <Textarea
                id="description"
                placeholder="Détails sur cette catégorie..."
                rows={2}
                {...register('description')}
                className={errors.description ? 'border-red-500' : ''}
              />
              {errors.description && (
                <p className="text-sm text-red-500">{errors.description.message}</p>
              )}
            </div>

            {/* Icon Selection */}
            <div className="space-y-2">
              <Label>Icône</Label>
              <div className="flex items-center gap-3">
                <div 
                  className="w-10 h-10 rounded-lg border-2 border-gray-300 flex items-center justify-center text-lg"
                  style={{ backgroundColor: selectedColor + '20' }}
                >
                  {selectedIcon}
                </div>
                <div className="flex-1">
                  <div className="grid grid-cols-8 gap-2 p-3 border rounded-lg max-h-32 overflow-y-auto">
                    {availableIcons.map((icon) => (
                      <button
                        key={icon}
                        type="button"
                        className={`w-8 h-8 rounded hover:bg-gray-100 flex items-center justify-center text-lg transition-colors ${
                          selectedIcon === icon ? 'bg-blue-100 border border-blue-300' : ''
                        }`}
                        onClick={() => setValue('iconName', icon)}
                      >
                        {icon}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            {/* Color Selection */}
            <div className="space-y-2">
              <Label>Couleur</Label>
              <div className="flex items-center gap-3">
                <div 
                  className="w-10 h-10 rounded-lg border-2 border-gray-300"
                  style={{ backgroundColor: selectedColor }}
                />
                <div className="flex-1">
                  <div className="grid grid-cols-10 gap-2 p-3 border rounded-lg">
                    {availableColors.map((color) => (
                      <button
                        key={color}
                        type="button"
                        className={`w-6 h-6 rounded border-2 transition-transform hover:scale-110 ${
                          selectedColor === color ? 'border-gray-800 scale-110' : 'border-gray-300'
                        }`}
                        style={{ backgroundColor: color }}
                        onClick={() => setValue('colorCode', color)}
                      />
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Annuler
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? (isEditMode ? 'Modification...' : 'Création...') : (isEditMode ? 'Modifier' : 'Créer')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}